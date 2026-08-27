import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const preloadPath = resolve("dist/preload/index.cjs");
const preload = await readFile(preloadPath, "utf8");

if (!preload.includes('require("electron")')) {
  throw new Error(`${preloadPath} must load Electron through CommonJS require()`);
}

if (/^\s*import\s/m.test(preload)) {
  throw new Error(`${preloadPath} cannot contain ESM imports while the renderer sandbox is enabled`);
}

console.log(`Verified sandbox-compatible preload: ${preloadPath}`);
