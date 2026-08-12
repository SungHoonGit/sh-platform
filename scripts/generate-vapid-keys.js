#!/usr/bin/env node
// VAPID 키 생성 스크립트
// 사용법: node generate-vapid-keys.js

const webpush = require('web-push');

const vapidKeys = webpush.generateVapidKeys();

console.log('=== VAPID Keys ===');
console.log('PUBLIC_KEY=' + vapidKeys.publicKey);
console.log('PRIVATE_KEY=' + vapidKeys.privateKey);
console.log('');
console.log('=== .env에 추가 ===');
console.log('WEBPUSH_VAPID_PUBLIC_KEY=' + vapidKeys.publicKey);
console.log('WEBPUSH_VAPID_PRIVATE_KEY=' + vapidKeys.privateKey);
