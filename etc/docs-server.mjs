/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

import { createReadStream, existsSync } from 'node:fs';
import { stat } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join, normalize, resolve, sep } from 'node:path';

const root = resolve(process.argv[2] ?? 'docs');
const port = Number(process.argv[3] ?? 3000);

const mediaTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.md', 'text/markdown; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.yaml', 'text/yaml; charset=utf-8'],
  ['.yml', 'text/yaml; charset=utf-8']
]);

function safePath(urlPath) {
  const pathname = decodeURIComponent(new URL(urlPath, `http://localhost:${port}`).pathname);
  const normalized = normalize(pathname).replace(/^(\.\.[/\\])+/, '');
  const filePath = resolve(join(root, normalized));
  return filePath === root || filePath.startsWith(root + sep) ? filePath : null;
}

async function resolveFile(urlPath) {
  const filePath = safePath(urlPath);
  if (!filePath) {
    return null;
  }

  if (!existsSync(filePath)) {
    return null;
  }

  const fileStat = await stat(filePath);
  if (fileStat.isDirectory()) {
    return join(filePath, 'index.html');
  }
  return filePath;
}

const server = createServer(async (request, response) => {
  try {
    const filePath = await resolveFile(request.url ?? '/');
    if (!filePath || !existsSync(filePath)) {
      response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
      response.end('Not found');
      return;
    }

    response.writeHead(200, {
      'content-type': mediaTypes.get(extname(filePath)) ?? 'application/octet-stream'
    });
    createReadStream(filePath).pipe(response);
  } catch (e) {
    response.writeHead(500, { 'content-type': 'text/plain; charset=utf-8' });
    response.end(String(e));
  }
});

server.listen(port, () => {
  console.log(`Serving ${root}`);
  console.log(`Open http://localhost:${port}`);
});
