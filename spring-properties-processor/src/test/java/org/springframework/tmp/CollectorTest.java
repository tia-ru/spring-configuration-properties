package org.springframework.tmp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CollectorTest {
    @Test
    public void test(){
        String r;
        r = intersect("cmj.a", "cmj.b");
        Assertions.assertEquals("cmj", r);
        r = intersect("cmj.ab", "cmj.ac");
        Assertions.assertEquals("cmj", r);
        r = intersect("cmj.ab", "cmj.abc");
        Assertions.assertEquals("cmj", r);
        r = intersect("cmj", "cmj");
        Assertions.assertEquals("cmj", r);

        r = intersect("cmj.ab", "cmj.ab.");
        Assertions.assertEquals("cmj.ab", r);

        r = intersect("", "cmj.abc");
        Assertions.assertEquals("", r);

        r = intersect("cm", "cmj");
        Assertions.assertEquals("", r);

    }

    private String intersect(String s1, String s2) {
        s1 += '.';
        s2 += '.';
        int len = Math.min(s1.length(), s2.length());
        int lastDotIdx = 0;
        for (int i = 0; i < len; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                len = i;
                break;
            }
            if (s1.charAt(i) == '.') {
                lastDotIdx = i;
            }
        }
        return s1.substring(0, lastDotIdx);
    }
}
