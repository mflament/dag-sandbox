import type {
    BaseNativeObject, BinaryFile, LayoutEntry, NativeClass, NativeEnum, NativeEnumConstant, NativeField, NativeStruct,
    NativeType, PrimitiveType
} from "./bin-file.ts";
import {ArrayBufferReader} from "./array-buffer-reader.ts";

function loadFile(file: File): Promise<ArrayBuffer> {
    return new Promise((resolve, reject) => {
        const fileReader = new FileReader();
        fileReader.readAsArrayBuffer(file);
        fileReader.onload = () => resolve(fileReader.result as ArrayBuffer);
        fileReader.onerror = () => reject(fileReader.error);
    });
}

const HEADER = 'NBIN';

function readHeader(reader: ArrayBufferReader) {
    const slice = reader.readSlice(4);
    if (slice.readString(4) != HEADER) throw Error("Invalid header");
}

function readEnumConstant(reader: ArrayBufferReader): NativeEnumConstant {
    const name = reader.readString();
    const value = reader.readInt();
    return {name, value};
}

type UnresolvedNativeType = NativeEnum | PrimitiveType | UnresolvedNativeObject;
type UnresolvedNativeObject = Omit<BaseNativeObject, 'fields'> & {
    objectType: 'struct' | 'class',
    fields: UnresolvedField[]
};

interface UnresolvedField {
    readonly name: string;
    readonly dims: number;
    readonly typeId: number;
}

function readNativeType(reader: ArrayBufferReader): UnresolvedNativeType {
    reader.readInt(); // skip size
    const id = reader.readInt();
    const name = reader.readString();
    const typeCode = reader.readByte();
    const fieldsCount = reader.readInt();

    function readField(reader: ArrayBufferReader): UnresolvedField {
        const name = reader.readString();
        const dims = reader.readInt();
        const typeId = reader.readInt();
        return {name, dims, typeId};
    }

    if (typeCode == 2) { // enum
        const constants: NativeEnumConstant[] = new Array(fieldsCount);
        for (let i = 0; i < fieldsCount; i++) constants[i] = readEnumConstant(reader);
        return {objectType: 'enum', id, name, constants};
    } else {
        const objectType = typeCode == 1 ? 'struct' : 'class';
        const fields: UnresolvedField[] = new Array(fieldsCount);
        for (let i = 0; i < fieldsCount; i++) {
            fields[i] = readField(reader);
        }
        return {objectType, id, name, fields};
    }
}

const PRIMITIVE_TYPES: PrimitiveType[] = [
    {objectType: 'primitive', id: -1, name: 'byte'},
    {objectType: 'primitive', id: -2, name: 'short'},
    {objectType: 'primitive', id: -3, name: 'int'},
    {objectType: 'primitive', id: -4, name: 'long'},
    {objectType: 'primitive', id: -5, name: 'float'},
    {objectType: 'primitive', id: -6, name: 'double'},
    {objectType: 'primitive', id: -7, name: 'boolean'},
];

function readNativeTypes(reader: ArrayBufferReader): Record<number, NativeType> {
    const count = reader.readInt();
    const unresolvedTypes: UnresolvedNativeType[] = [...PRIMITIVE_TYPES];
    for (let i = 0; i < count; i++) {
        unresolvedTypes.push(readNativeType(reader));
    }

    const nativeTypes: Record<number, NativeType> = {};
    PRIMITIVE_TYPES.forEach(t => nativeTypes[t.id] = t);

    function resolveNativeType(id: number): NativeType {
        const nativeType = nativeTypes[id];
        if (nativeType)
            return nativeType;

        const unresolvedType = unresolvedTypes.find(t => t.id === id);
        if (!unresolvedType) throw new Error("type with id " + id + " not found");
        return createNativeType(unresolvedType);
    }

    function createNativeField(field: UnresolvedField): NativeField {
        const type = resolveNativeType(field.typeId);
        if (field.dims == 0)
            return {fieldType: 'value', name: field.name, type};
        return {fieldType: 'array', name: field.name, dims: field.dims, componentType: type};
    }

    function createNativeType(type: UnresolvedNativeType): NativeType {
        const current = nativeTypes[type.id];
        if (current)
            return current;

        if (type.objectType == 'enum' || type.objectType == 'primitive') {
            nativeTypes[type.id] = type;
            return type;
        }

        const nativeType: NativeClass | NativeStruct = {
            objectType: type.objectType,
            id: type.id,
            name: type.name,
            fields: []
        };
        nativeTypes[nativeType.id] = nativeType;
        for (let field of type.fields) {
            nativeType.fields.push(createNativeField(field));
        }
        return nativeType;
    }

    unresolvedTypes.forEach(createNativeType);
    return nativeTypes;
}

function readLayoutEntry(reader: ArrayBufferReader, nativeTypes: Record<number, NativeType>): LayoutEntry {
    const typeId = reader.readInt();
    const type = nativeTypes[typeId];
    if (!type) throw Error("Unresolved type id " + typeId);

    const offset = reader.readLong();
    const size = reader.readInt();
    const length = reader.readInt();
    if (size < 0)
        return {entryType: 'array', offset, size: -size, type, length};
    if (type.objectType !== 'class')
        throw new Error("Invalid layout entry : not an object");
    return {entryType: "object", offset, size, type};
}

function readLayout(reader: ArrayBufferReader, nativeTypes: Record<number, NativeType>): LayoutEntry[] {
    const count = reader.readInt();
    const entries: LayoutEntry[] = new Array(count);
    for (let i = 0; i < count; i++) {
        entries[i] = readLayoutEntry(reader, nativeTypes);
    }
    return entries;
}

export async function readBinFile(file: File): Promise<BinaryFile> {
    const buffer = await loadFile(file);
    const reader = new ArrayBufferReader(buffer);
    readHeader(reader);
    const nativeTypes = readNativeTypes(reader);
    const layout = readLayout(reader, nativeTypes);
    return {nativeTypes, layout, reader};
}
