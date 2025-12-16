// walkie-sfu/server.js
// Minimal mediasoup-based SFU for a single-channel walkie-talkie.
// - Signaling via WebSocket JSON
// - OPUS audio only (router configured accordingly)
// - Server enforces PTT speaker lock -> only one producer at a time
// - Server does not decode audio (acts as SFU relay)

const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const mediasoup = require('mediasoup');
const { v4: uuidv4 } = require('uuid');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Serve public files (client)
app.use('/', express.static(__dirname + '/public'));

// Global mediasoup objects
let worker;
let router;
const peers = new Map(); // peerId -> { ws, transports:{}, producers:{}, consumers:{}, deviceRtpCapabilities }
let currentSpeaker = null; // peerId of current speaker (PTT lock)

(async () => {
  // Create a mediasoup Worker
  worker = await mediasoup.createWorker({
    rtcMinPort: 10000,
    rtcMaxPort: 10100
  });

  worker.on('died', () => {
    console.error('mediasoup worker died');
    process.exit(1);
  });

  // Create router with Opus only
  router = await worker.createRouter({
    mediaCodecs: [
      {
        kind: 'audio',
        mimeType: 'audio/opus',
        clockRate: 48000,
        channels: 2
      }
    ]
  });

  console.log('Mediasoup worker and router created');

  // WebSocket signaling handler
  wss.on('connection', (ws) => {
    const peerId = uuidv4();
    peers.set(peerId, { ws, transports: new Map(), producers: new Map(), consumers: new Map(), deviceRtpCapabilities: null });
    console.log('Peer connected', peerId);

    ws.send(JSON.stringify({ action: 'peerId', peerId }));

    ws.on('message', async (message) => {
      try {
        const msg = JSON.parse(message);
        await handleMessage(peerId, msg);
      } catch (err) {
        console.error('Failed to handle message', err);
      }
    });

    ws.on('close', () => {
      console.log('Peer disconnected', peerId);
      cleanupPeer(peerId);
    });
  });

  // Start server
  const PORT = process.env.PORT || 3001;
  server.listen(PORT, () => console.log(`Walkie SFU running on port ${PORT}`));
})();

async function handleMessage(peerId, msg) {
  const peer = peers.get(peerId);
  if (!peer) return;
  const ws = peer.ws;

  switch (msg.action) {
    case 'getRouterRtpCapabilities':
      ws.send(JSON.stringify({ action: 'routerRtpCapabilities', data: router.rtpCapabilities }));
      break;

    case 'createWebRtcTransport': {
      // Create a WebRtcTransport and return parameters to client
      const transport = await router.createWebRtcTransport({
        listenIps: [{ ip: '0.0.0.0', announcedIp: null }],
        enableUdp: true,
        enableTcp: false,
        preferUdp: true,
        initialAvailableOutgoingBitrate: 100000
      });

      peer.transports.set(transport.id, transport);

      ws.send(JSON.stringify({
        action: 'createWebRtcTransportResult',
        data: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters
        }
      }));
      break;
    }

    case 'connectTransport': {
      const { transportId, dtlsParameters } = msg.data;
      const transport = peer.transports.get(transportId);
      if (!transport) { ws.send(JSON.stringify({ action: 'error', message: 'transport not found' })); return; }
      await transport.connect({ dtlsParameters });
      ws.send(JSON.stringify({ action: 'connectTransportResult', transportId }));
      break;
    }

    case 'produce': {
      // Enforce speaker lock: only currentSpeaker may produce
      if (currentSpeaker !== peerId) {
        ws.send(JSON.stringify({ action: 'produceError', message: 'BUSY' }));
        return;
      }

      const { transportId, kind, rtpParameters } = msg.data;
      const transport = peer.transports.get(transportId);
      if (!transport) { ws.send(JSON.stringify({ action: 'error', message: 'transport not found' })); return; }

      const producer = await transport.produce({ kind, rtpParameters });
      peer.producers.set(producer.id, producer);

      // Notify others about new producer (they will consume)
      broadcastExcept(peerId, JSON.stringify({ action: 'newProducer', data: { producerId: producer.id, peerId } }));

      ws.send(JSON.stringify({ action: 'produceResult', data: { producerId: producer.id } }));

      // Handle producer close
      producer.on('transportclose', () => {
        peer.producers.delete(producer.id);
      });
      producer.on('close', () => {
        peer.producers.delete(producer.id);
      });

      break;
    }

    case 'consume': {
      const { consumerTransportId, producerId, rtpCapabilities } = msg.data;
      // Check if router can consume
      if (!router.canConsume({ producerId, rtpCapabilities })) {
        ws.send(JSON.stringify({ action: 'consumeDenied', message: 'cannot consume' }));
        return;
      }

      const transport = peer.transports.get(consumerTransportId);
      if (!transport) { ws.send(JSON.stringify({ action: 'error', message: 'transport not found' })); return; }

      const consumer = await transport.consume({ producerId, rtpCapabilities, paused: false });
      peer.consumers.set(consumer.id, consumer);

      ws.send(JSON.stringify({ action: 'consumeResult', data: {
        consumerId: consumer.id,
        producerId,
        kind: consumer.kind,
        rtpParameters: consumer.rtpParameters
      } }));

      consumer.on('transportclose', () => peer.consumers.delete(consumer.id));
      consumer.on('producerclose', () => {
        peer.consumers.delete(consumer.id);
        ws.send(JSON.stringify({ action: 'producerClosed', data: { producerId } }));
      });

      break;
    }

    case 'requestLock': {
      // PTT lock: if no current speaker, grant the lock to this peer
      if (!currentSpeaker) {
        currentSpeaker = peerId;
        broadcastAll(JSON.stringify({ action: 'lockGranted', data: { peerId } }));
        ws.send(JSON.stringify({ action: 'requestLockResult', data: { granted: true } }));
      } else {
        ws.send(JSON.stringify({ action: 'requestLockResult', data: { granted: false, reason: 'BUSY' } }));
      }
      break;
    }

    case 'releaseLock': {
      if (currentSpeaker === peerId) {
        currentSpeaker = null;
        broadcastAll(JSON.stringify({ action: 'lockReleased', data: { peerId } }));
        ws.send(JSON.stringify({ action: 'releaseLockResult', data: { success: true } }));
      } else {
        ws.send(JSON.stringify({ action: 'releaseLockResult', data: { success: false, reason: 'not-owner' } }));
      }
      break;
    }

    case 'getProducers': {
      // Return simple list of producers available
      const producers = [];
      for (const [otherPeerId, otherPeer] of peers.entries()) {
        if (otherPeerId === peerId) continue;
        for (const [prodId] of otherPeer.producers) {
          producers.push({ producerId: prodId, peerId: otherPeerId });
        }
      }
      ws.send(JSON.stringify({ action: 'producersList', data: producers }));
      break;
    }

    default:
      ws.send(JSON.stringify({ action: 'error', message: 'unknown action ' + msg.action }));
  }
}

function broadcastAll(message) {
  for (const { ws } of peers.values()) {
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(message);
  }
}

function broadcastExcept(exceptPeerId, message) {
  for (const [peerId, { ws }] of peers.entries()) {
    if (peerId === exceptPeerId) continue;
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(message);
  }
}

async function cleanupPeer(peerId) {
  const peer = peers.get(peerId);
  if (!peer) return;

  // Release any producers
  for (const [prodId, producer] of peer.producers) {
    try { await producer.close(); } catch (e) {}
  }
  // Close transports
  for (const [tId, transport] of peer.transports) {
    try { transport.close(); } catch (e) {}
  }

  // If this peer held the lock, release it
  if (currentSpeaker === peerId) {
    currentSpeaker = null;
    broadcastAll(JSON.stringify({ action: 'lockReleased', data: { peerId } }));
  }

  peers.delete(peerId);
}
