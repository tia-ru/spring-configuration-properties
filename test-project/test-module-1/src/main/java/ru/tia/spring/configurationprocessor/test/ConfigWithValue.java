package ru.tia.spring.configurationprocessor.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

@PropertySource("${cmj.prop.source.file}")
public class ConfigWithValue {

    /**
     * @deprecated use prop2 instead
     */
    @Deprecated
    @Value("${cmj.prop1}")
    String myProp1;

    @Value("cmj.must-be-skipped")
    String skipped;

    @Value("${cmj.bool}")
    boolean bool;

    @Value("${cmj2.bool}")
    boolean bool2;
    /**
     * A prop with default
     */
    @Value("${cmj2.not-exist:default}")
    String myDefaultVal;

    @Value("#{'${cmj.auth.alter.type}'.toUpperCase()}")
    String spel;

    @Value("${cmj.not-exist-int:100}")
    int myDefaultInt;

    /**
     * param1 фывяывыфф
     * param2 ыфвфывфывфыв
     * @deprecated дддддд
     *
     * @param myParam1 @deprecated asdd
     * @param myParam2
     */
    void method(
            @Value("${param1}")
            String myParam1,

            @Deprecated
            @Value("${param2}")
            String myParam2 ){}

    @Deprecated
    @Value("setter1")
    void setSetter(String s){}

    @Value("${cmj.part1:${cmj.part1.1:default1.1}} plus ${cmj.part2:default2}")
    String composite;

}
