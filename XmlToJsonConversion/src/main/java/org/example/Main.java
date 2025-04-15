package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import com.fasterxml.jackson.databind.JsonNode;

public class Main {
    public static void main(String[] args) {
        String xml = """ 
             <?xml version="1.0" encoding="UTF-8"?>
             <Response>
                    <ResultBlock>
                        <ErrorWarnings>
                            <Errors errorCount="0" />
                            <Warnings warningCount="1">
                                <Warning>
                                    <Number>102001</Number>
                                    <Message>Minor mismatch in address</Message>
                                    <Values>
                                        <Value>Bellandur</Value>
                                        <Value>Bangalore</Value>
                                    </Values>
                                </Warning>
                            </Warnings>
                        </ErrorWarnings>
                        <MatchDetails>
                            <Match>
                                <Entity>John</Entity>
                                <MatchType>Exact</MatchType>
                                <Score>35</Score>
                            </Match>
                            <Match>
                                <Entity>Doe</Entity>
                                <MatchType>Exact</MatchType>
                                <Score>50</Score>
                            </Match>
                        </MatchDetails>
                        <API>
                            <RetStatus>SUCCESS</RetStatus>
                            <ErrorMessage />
                            <SysErrorCode />
                            <SysErrorMessage />
                        </API>
                    </ResultBlock>
                </Response>
              """;


        try {
            JsonNode json = XmlToJsonConverterUtility.convert(xml);
            System.out.println("Converted JSON:\n" + json.toPrettyString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
