package com.abetterday.task.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** deleted_yn 컬럼의 'Y'/'N'과 boolean 변환을 한 곳에서 처리하는 컨버터. */
@Converter
public class DeletedYnConverter implements AttributeConverter<Boolean, Character> {

    /** boolean을 DB 컬럼 값('Y' 또는 'N')으로 변환한다. */
    @Override
    public Character convertToDatabaseColumn(Boolean attribute) {
        return Boolean.TRUE.equals(attribute) ? 'Y' : 'N';
    }

    /** DB 컬럼 값('Y' 또는 'N')을 boolean으로 변환한다. */
    @Override
    public Boolean convertToEntityAttribute(Character dbData) {
        return dbData != null && dbData == 'Y';
    }
}
