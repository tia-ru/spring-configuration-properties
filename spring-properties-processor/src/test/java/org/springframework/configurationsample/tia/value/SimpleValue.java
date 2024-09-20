package org.springframework.configurationsample.tia.value;

import java.util.Set;

import org.springframework.configurationsample.tia.GetMapping;
import org.springframework.configurationsample.tia.PropertySource;
import org.springframework.configurationsample.tia.RequestMapping;
import org.springframework.configurationsample.tia.Value;

@PropertySource("${prop.source.file}")
public class SimpleValue {

    /**
     * @deprecated use prop2 instead
     */
    @Deprecated
    @Value("${prop1}")
    String myProp1;

    @Value("must-be-skipped")
    String skipped;

    @Value("${bool}")
    boolean bool;

    @Value("${bool}")
    boolean bool2;
    /**
     * A prop with default
     */
    @Value("${not-exist:default}")
    String myDefaultVal;

    @Value("#{'${cmj.auth.alter.type}'.toUpperCase()}")
    String spel;

    @Value("${not-exist-int:100}")
    int myDefaultInt;

    void method(@Deprecated @Value("${param1}") String myParam1){}

    @Deprecated
    @Value("setter1")
    void setSetter(String s){}

    @Value("${part1:${part1.1:default1.1}} plus ${part2:default2}")
    String composite;

    @Value("${enum}")
    AnEnum anEnum;

    @Value("${enum-set}")
    Set<AnEnum> enumSet;

    @RequestMapping(method = AnEnum.E1, path = "${profile_name}")
    void process(){}

    @GetMapping(path = "${get.param}")
    void get() {

    }
}
