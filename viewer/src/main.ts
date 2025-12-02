import './style.css'
import {readBinFile} from "./bin-file-reader.ts";

function queryElement<T extends HTMLElement>(selector: string): T {
    const e = document.querySelector<HTMLElement>(selector);
    if (e === null) throw new Error(selector + " not found");
    return e as T;
}

function getFile(e: DragEvent): DataTransferItem | null {
    if (!e.dataTransfer) return null;
    const fileItems = [...e.dataTransfer.items].filter(item => item.kind === "file");
    return fileItems.length > 0 ? fileItems[0] : null;
}

async function setup() {
    const dropZone = queryElement('#dropzone');

    function drop(e: DragEvent) {
        const item = getFile(e);
        if (!item) return;
        e.preventDefault();
        const file = item.getAsFile();
        if (file) {
            readBinFile(file).then(bf => console.log(bf));
        }
    }

    function dragover(e: DragEvent) {
        if (!getFile(e)) return;
        e.preventDefault();
        e.dataTransfer!.dropEffect = "copy";
    }

    function dragoverWindow(e: DragEvent) {
        if (!getFile(e)) return;
        e.preventDefault();
        if (!dropZone.contains(e.target as Node)) {
            e.dataTransfer!.dropEffect = "none";
        }
    }

    dropZone.addEventListener('drop', drop);
    dropZone.addEventListener('dragover', dragover);
    window.addEventListener('dragover', dragoverWindow);
}

queryElement('#app').innerHTML = `
    <div id="dropzone"></div>
`

setup().catch(e => console.error(e));