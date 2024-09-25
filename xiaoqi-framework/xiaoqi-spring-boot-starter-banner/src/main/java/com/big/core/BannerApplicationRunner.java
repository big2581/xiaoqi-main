package com.big.core;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * TODO
 *
 * @author Yin
 * @date 2024-09-24 15:16
 */
public class BannerApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("      .---- -. -. .  .   .        .                                           \n" +
                "     ( .',----- - - ' '      '                                         __     \n" +
                "      \\_/      ;--:-          __--------------------___  ____=========_||___  \n" +
                "     __U__n_^_''__[.  ooo___  | |_!_||_!_||_!_||_!_| |   |..|_i_|..|_i_|..|   \n" +
                "   c(_ ..(_ ..(_ ..( /,,,,,,] | |___||___||___||___| |   |                |   \n" +
                "   ,_\\___________'_|,L______],|______________________|_i,!________________!_i \n" +
                "  /;_(@)(@)==(@)(@)   (o)(o)      (o)^(o)--(o)^(o)          (o)(o)-(o)(o)     \n" +
                "\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"~\"\"\"");
    }
}
