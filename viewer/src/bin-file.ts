import type {ArrayBufferReader} from "./array-buffer-reader.ts";

export interface BinaryFile {
    readonly nativeTypes: Record<number, NativeType>;
    readonly layout: LayoutEntry[];
    readonly reader: ArrayBufferReader;
}

//// native types

export type NativeType = NativeClass | NativeStruct | NativeEnum | PrimitiveType;

export interface BaseNativeType {
    readonly objectType: string;
    readonly id: number;
    readonly name: string;
}

export interface PrimitiveType extends BaseNativeType {
    readonly objectType: 'primitive';
    name: 'byte' | 'short' | 'int' | 'long' | 'float' | 'double' | 'boolean';
}

export interface BaseNativeObject extends BaseNativeType {
    readonly fields: NativeField[];
}

export interface NativeClass extends BaseNativeObject {
    readonly objectType: 'class';
}

export interface NativeStruct extends BaseNativeObject {
    readonly objectType: 'struct';
}

export interface NativeEnum extends BaseNativeType {
    readonly objectType: 'enum';
    readonly constants: NativeEnumConstant[];
}

export interface NativeEnumConstant {
    readonly name: string;
    readonly value: number;
}

export interface BaseNativeField {
    readonly fieldType: string;
    readonly name: string;
}

export interface NativeArrayField extends BaseNativeField {
    readonly fieldType: 'array';
    readonly dims: number;
    readonly componentType: NativeType;
}

export interface NativeValueField extends BaseNativeField {
    readonly fieldType: 'value';
    readonly type: NativeType;
}

export type NativeField = NativeArrayField | NativeValueField;

//// layout

export interface BaseLayoutEntry {
    readonly entryType: string;
    readonly offset: BigInt;
    readonly size: number;
}

export interface ObjectLayoutEntry extends BaseLayoutEntry {
    readonly entryType: 'object';
    readonly type: NativeClass;
}

export interface ArrayLayoutEntry extends BaseLayoutEntry {
    readonly entryType: 'array';
    readonly type: NativeType;
    readonly length: number;
}

export type LayoutEntry = ObjectLayoutEntry | ArrayLayoutEntry;
