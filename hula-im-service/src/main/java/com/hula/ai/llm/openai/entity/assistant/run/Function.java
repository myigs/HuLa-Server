package com.hula.ai.llm.openai.entity.assistant.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Data
@Slf4j
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Function implements Serializable {

    /**
     * The name of the function.
     */
    private String name;
    /**
     * The arguments passed to the function.
     */
    private String arguments;
    /**
     * The output of the function. This will be null if the outputs have not been submitted yet.
     * 函数的输出。如果尚未提交输出，则该值为空。
     */
    private String output;


}
