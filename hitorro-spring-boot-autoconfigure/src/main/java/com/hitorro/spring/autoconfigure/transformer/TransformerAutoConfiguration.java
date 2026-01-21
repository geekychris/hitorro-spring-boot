/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.spring.autoconfigure.transformer;

import com.hitorro.spring.autoconfigure.dms.DMSAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Hitorro Transformer REST API
 */
@AutoConfiguration
@AutoConfigureAfter(DMSAutoConfiguration.class)
@ConditionalOnClass(name = "com.hitorro.basedms.transformer.TransformerService")
@ConditionalOnProperty(prefix = "hitorro.transformer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransformerAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformerAutoConfiguration.class);
    
    public TransformerAutoConfiguration() {
        logger.info("╔══════════════════════════════════════════════════════╗");
        logger.info("║  TransformerAutoConfiguration CONSTRUCTOR CALLED   ║");
        logger.info("╚══════════════════════════════════════════════════════╝");
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.transformer.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RenditionTransformationController renditionTransformationController(
            com.hitorro.spring.autoconfigure.dms.DMSSessionFactory sessionFactory) {

        logger.info("╔══════════════════════════════════════════════════════╗");
        logger.info("║  Registering RenditionTransformationController      ║");
        logger.info("╚══════════════════════════════════════════════════════╝");
        return new RenditionTransformationController(sessionFactory);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "hitorro.transformer.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DocumentContentController documentContentController() {
        logger.info("╔══════════════════════════════════════════════════════╗");
        logger.info("║  Registering DocumentContentController              ║");
        logger.info("╚══════════════════════════════════════════════════════╝");
        return new DocumentContentController();
    }
}
