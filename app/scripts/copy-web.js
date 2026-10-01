// 저장소 루트의 웹 앱(index.html, lang/)을 앱용 www 폴더로 복사
const fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..', '..'), www = path.join(__dirname, '..', 'www');
fs.rmSync(www, { recursive: true, force: true });
fs.mkdirSync(www, { recursive: true });
fs.copyFileSync(path.join(root, 'index.html'), path.join(www, 'index.html'));
fs.cpSync(path.join(root, 'lang'), path.join(www, 'lang'), { recursive: true });
console.log('copied web app to', www);
