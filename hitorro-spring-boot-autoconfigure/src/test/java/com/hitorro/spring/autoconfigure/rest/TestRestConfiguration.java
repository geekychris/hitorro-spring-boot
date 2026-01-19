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
package com.hitorro.spring.autoconfigure.rest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Minimal test configuration for REST controller tests.
 * Uses Spring Boot's auto-configuration for most beans.
 */
@SpringBootApplication
public class TestRestConfiguration {
    
    @Bean
    public HitorroRestControllerTest.StreamingCsvCommand streamingCsvCommand() {
        return new HitorroRestControllerTest.StreamingCsvCommand();
    }
    
    @Bean
    public HitorroRestControllerTest.CustomHeadersCommand customHeadersCommand() {
        return new HitorroRestControllerTest.CustomHeadersCommand();
    }
    
    @Bean
    public HitorroRestControllerTest.LargeDatasetCommand largeDatasetCommand() {
        return new HitorroRestControllerTest.LargeDatasetCommand();
    }
    
    @Bean
    public HitorroRestControllerTest.RequestInfoCommand requestInfoCommand() {
        return new HitorroRestControllerTest.RequestInfoCommand();
    }
    
    @Bean
    public HitorroRestControllerTest.ResponseHeadersCommand responseHeadersCommand() {
        return new HitorroRestControllerTest.ResponseHeadersCommand();
    }
    
    @Bean
    public HitorroRestControllerTest.MultiMethodCommand multiMethodCommand() {
        return new HitorroRestControllerTest.MultiMethodCommand();
    }
}
