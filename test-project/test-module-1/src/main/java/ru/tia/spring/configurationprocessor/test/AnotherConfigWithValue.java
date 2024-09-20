package ru.tia.spring.configurationprocessor.test;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import ru.tia.spring.configurationprocessor.test.util.AnEnum;

@PropertySource("${cmj.prop.source.file}")

public class AnotherConfigWithValue {

    @Value("${cmj.enum}")
    AnEnum anEnum;

    @Value("${cmj.enum-set}")
    Set<AnEnum> enumSet;
}
