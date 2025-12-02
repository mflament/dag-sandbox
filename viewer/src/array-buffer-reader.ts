export class ArrayBufferReader {
    private readonly dv: DataView<ArrayBuffer>;
    private readonly textDecoder = new TextDecoder("utf-8");

    offset = 0;

    constructor(buffer: ArrayBuffer) {
        this.dv = new DataView(buffer);
    }

    readByte(): number {
        return this.dv.getInt8(this.offset++);
    }

    readShort(): number {
        return this.readAndAdvance(this.dv.getInt16, 2);
    }

    readInt(): number {
        return this.readAndAdvance(this.dv.getInt32, 4);
    }

    readLong(): BigInt {
        return this.readAndAdvance(this.dv.getBigInt64, 8);
    }

    readFloat(): number {
        return this.readAndAdvance(this.dv.getFloat32, 4);
    }

    readDouble(): number {
        return this.readAndAdvance(this.dv.getFloat64, 8);
    }

    readBoolean(): boolean {
        return this.readAndAdvance(this.dv.getInt8, 2) != 0;
    }

    readSlice(size: number): ArrayBufferReader {
        const slice = this.dv.buffer.slice(this.offset, this.offset + size);
        this.offset += size;
        return new ArrayBufferReader(slice);
    }

    readString(size?: number): string {
        size = size === undefined ? this.readInt() : size;
        const res = this.textDecoder.decode(this.dv.buffer.slice(this.offset, this.offset + size));
        this.offset += size;
        return res;
    }

    private readAndAdvance<T>(read: (offset: number, littleEndian: boolean) => T, size: number): T {
        try {
            const res = read.call(this.dv, this.offset, true);
            this.offset += size;
            return res;
        } catch (e) {
            throw e;
        }
    }
}