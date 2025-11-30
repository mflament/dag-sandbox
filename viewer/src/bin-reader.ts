export function readBinFile(file: File): BinaryFile {
    console.log(file.name);
    return undefined as any;
}

export interface BinaryFile {
    readonly version: number;
    readonly model: SerializedModel;
    readonly instance: BinInstance;
}

export type BinInstance =
    | BinaryPrimitive
    | BinaryString
    | BinaryEnum
    | BinaryObject
    | BinaryArray
    ;

export class BaseBinaryInstance<T extends SerializedType> {
    readonly id: number;
    readonly type: T;

    constructor(id: number, type: T) {
        this.id = id;
        this.type = type;
    }
}

export class BinaryPrimitive extends BaseBinaryInstance<SerializedPrimitive> {
    readonly value: number | boolean | string;

    constructor(id: number, type: SerializedPrimitive, value: number | boolean | string) {
        super(id, type);
        this.value = value;
    }
}

export class BinaryString extends BaseBinaryInstance<SerializedString> {
    readonly value: string;

    constructor(id: number, type: SerializedString, value: string) {
        super(id, type);
        this.value = value;
    }
}

export class BinaryEnum extends BaseBinaryInstance<SerializedEnum> {
    readonly ordinal: number;

    constructor(id: number, type: SerializedEnum, ordinal: number) {
        super(id, type);
        this.ordinal = ordinal;
    }

    name(): string {
        return this.type.constants[this.ordinal];
    }
}

export type BinaryArrayContent = ArrayBufferLike | string[] | BinaryObject[] | BinaryEnum[];

export class BinaryArray<A extends BinaryArrayContent> extends BaseBinaryInstance<SerializedArrayType> {
    readonly length: number;

    constructor(id: number, type: SerializedArrayType, length: number) {
        super(id, type);
        this.length = length;
    }

    readContent(): Promise<A> {
        return Promise.reject("TODO");
    }
}

export class BinaryObject extends BaseBinaryInstance<SerializedClass> {

    constructor(id: number, type: SerializedClass) {
        super(id, type);
    }
}

export type SerializedModel = Record<string, SerializedType>;

export interface BaseSerializedType {
    readonly type: string;
}

export interface SerializedPrimitive extends BaseSerializedType {
    readonly type: 'primitive';
    readonly name: PrimitiveName;
}

export type PrimitiveName =
    | 'byte'
    | 'short'
    | 'int'
    | 'long'
    | 'float'
    | 'double'
    | 'boolean'
    | 'char'
    ;

export interface SerializedString extends BaseSerializedType {
    readonly type: 'string';
}

export interface SerializedClass extends BaseSerializedType {
    readonly type: 'class';
    readonly className: string;
    readonly fields: SerializedField[];
}

export interface SerializedField {
    readonly fieldName: string;
    readonly type: SerializedType;
}

export interface SerializedArrayType extends BaseSerializedType {
    readonly type: 'array';
    readonly componentType: SerializedType;
}

export interface SerializedParameterizedType extends BaseSerializedType {
    readonly type: 'parameterizedType';
    readonly rawType: SerializedType;
    readonly typeArguments: SerializedType[];
}

export interface SerializedEnum extends BaseSerializedType {
    readonly type: 'enum';
    readonly className: string;
    readonly constants: string[];
}

export type SerializedType =
    | SerializedPrimitive
    | SerializedString
    | SerializedEnum
    | SerializedClass
    | SerializedArrayType
    | SerializedParameterizedType
    ;

export function getTypeName(type: SerializedType): string {
    switch (type.type) {
        case "primitive":
            return type.name;
        case "string":
            return 'string';
        case "class":
        case "enum":
            return type.className;
        case "array":
            return getTypeName(type.componentType) + "[]";
        case "parameterizedType":
            return getTypeName(type.rawType) + '<' + type.typeArguments.map(getTypeName).join(', ') + '>';
    }
}

export class BinFile {
    private readonly file: File;
    readonly version: number;
    readonly size: number;

    constructor(file: File) {
        this.file = file;
    }


}

export interface BinObject extends BinInstance {
    // readonly fields: Record<string, any>;
}