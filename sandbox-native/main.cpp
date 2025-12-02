#include "jna_sandbox.h"
#include <string.h> 
#include <errno.h> 

void printTestObject(struct TestObject* testObject, char* dst, int maxLength)
{
	snprintf(dst, maxLength, "%zu,%d,%d,%d,%lld,%f,%lf,%d,%d,{%d,%f,%d},{%zu}",
		testObject->_typeId,
		testObject->aByte,
		testObject->aShort,
		testObject->anInt,
		testObject->aLong,
		testObject->aFloat,
		testObject->aDouble,
		testObject->aBoolean,
		testObject->anEnum,
		testObject->aStruct.anInt, testObject->aStruct.aFloat, testObject->aStruct.aNativeEnum,
		testObject->objectWithArrays->_typeId);
}

void printTestObjectWithArrays(struct TestObject* testObject, int array, int length, char* dst, int maxLength)
{
	struct TestObjectWithArrays* oa = testObject->objectWithArrays;
	int strLength = 0;
	const char* format;
	for (int i = 0; i < length && strLength < maxLength; i++)
	{
		switch (array)
		{
		case 0:
			format = i < length - 1 ? "%d," : "%d";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aByteArray[i]);
			break;
		case 1:
			format = i < length - 1 ? "%d," : "%d";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aShortArray[i]);
			break;
		case 2:
			format = i < length - 1 ? "%d," : "%d";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->anIntArray[i]);
			break;
		case 3:
			format = i < length - 1 ? "%lld," : "%lld";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aLongArray[i]);
			break;
		case 4:
			format = i < length - 1 ? "%f," : "%f";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aFloatArray[i]);
			break;
		case 5:
			format = i < length - 1 ? "%lf," : "%lf";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aDoubleArray[i]);
			break;
		case 6:
			format = i < length - 1 ? "%d," : "%d";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->aBooleanArray[i]);
			break;
		case 7:
			format = i < length - 1 ? "%d," : "%d";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, oa->anEnumArray[i]);
			break;
		case 8:
			format = i < length - 1 ? "%zu," : "%zu";
			strLength += snprintf(dst + strLength, maxLength - strLength, format, (uint64_t)oa->aReferenceArray[i]);
			break;
		}
	}
}