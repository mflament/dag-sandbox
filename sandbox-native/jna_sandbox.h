#pragma once

#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>

struct BaseObject {
	uint64_t _typeId;
};

enum TestEnum {
	TestEnum_A, TestEnum_B, TestEnum_C, TestEnum_D
};

enum TestNativeEnum {
	TestNativeEnum_A = 5,
	TestNativeEnum_B = 10,
	TestNativeEnum_C = 20,
	TestNativeEnum_D = 42
};

struct TestStruct {
	int32_t anInt;
	float aFloat;
	enum TestNativeEnum aNativeEnum;
};

struct TestObjectWithArrays : BaseObject {
	char* aByteArray;
	int16_t* aShortArray;
	int32_t* anIntArray;
	int64_t* aLongArray;
	float* aFloatArray;
	double* aDoubleArray;
	bool* aBooleanArray;
	enum TestEnum* anEnumArray;
	struct TestStruct* aStructArray;
	void** aReferenceArray;
};

struct TestObject;

struct TestObject : BaseObject {
	struct BaseObject;
	char aByte;
	int16_t aShort;
	int32_t anInt;
	int64_t aLong;
	float aFloat;
	double aDouble;
	bool aBoolean;
	enum TestEnum anEnum;
	float* aFloatArray;
	struct TestStruct aStruct;
	struct TestObjectWithArrays* objectWithArrays;
	struct TestObject* aTestObject;
};



#ifdef _WIN32
#define DLL_EXPORT __declspec(dllexport)
#endif // _WIN32

#ifdef __cplusplus
extern "C" {
#endif

DLL_EXPORT void printTestObject(struct TestObject* testObject, char* dst, int maxLength);
DLL_EXPORT void printTestObjectWithArrays(struct TestObject* testObject, int array, int length, char* dst, int maxLength);
#ifdef __cplusplus
}
#endif
