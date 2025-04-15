package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;

public class XmlToJsonConverterUtility
{
    private static final Logger logger = LoggerFactory.getLogger(XmlToJsonConverterUtility.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode convert(String xml) throws Exception {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            doc.getDocumentElement().normalize();

            if (doc.getElementsByTagName("Response").item(0) == null) {
                throw new IllegalArgumentException("Missing 'Response' element in XML.");
            }

            JsonNode fullJson = xmlToJson(doc.getDocumentElement());
            JsonNode resultBlock = fullJson.path("ResultBlock");

            if (resultBlock.isMissingNode() || !resultBlock.isObject()) {
                throw new IllegalArgumentException("Missing 'ResultBlock' in XML.");
            }

            ObjectNode rootNode = mapper.createObjectNode();
            ObjectNode responseNode = rootNode.putObject("Response");
            ObjectNode resultBlockFinal = mapper.createObjectNode();

            // MatchSummary
            BigInteger totalScore = getTotalScore(resultBlock.path("MatchDetails").path("Match"));
            resultBlockFinal.putObject("MatchSummary").put("TotalMatchScore", totalScore.toString());

            // ErrorWarnings & MatchDetails
            resultBlockFinal.set("ErrorWarnings", nullIfMissing(resultBlock, "ErrorWarnings"));
            resultBlockFinal.set("MatchDetails", nullIfMissing(resultBlock, "MatchDetails"));

            // API
            JsonNode apiNode = resultBlock.path("API");
            ObjectNode api = mapper.createObjectNode();
            api.put("RetStatus", getNullableText(apiNode, "RetStatus"));
            api.set("ErrorMessage", getNullableNode(apiNode, "ErrorMessage"));
            api.set("SysErrorCode", getNullableNode(apiNode, "SysErrorCode"));
            api.set("SysErrorMessage", getNullableNode(apiNode, "SysErrorMessage"));
            resultBlockFinal.set("API", api);

            responseNode.set("ResultBlock", resultBlockFinal);
            return rootNode;

        } catch (Exception e) {
            logger.error("Failed to convert XML to JSON: {}", e.getMessage(), e);
            throw new Exception("Conversion error: " + e.getMessage(), e);
        }
    }

    private static BigInteger getTotalScore(JsonNode matchNode) {
        if (matchNode.isArray()) {
            BigInteger sum = BigInteger.ZERO;
            for (JsonNode match : matchNode) sum = sum.add(getScore(match));
            return sum;
        }
        return getScore(matchNode);
    }

    private static BigInteger getScore(JsonNode node) {
        try {
            return new BigInteger(node.path("Score").asText("0").trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid Score value: {}", node, e);
            return BigInteger.ZERO;
        }
    }

    private static JsonNode xmlToJson(Element element) {
        ObjectNode node = mapper.createObjectNode();
        NodeList children = element.getChildNodes();
        boolean hasElementChild = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                hasElementChild = true;
                String tag = ((Element) child).getTagName();
                JsonNode childJson = xmlToJson((Element) child);

                if (node.has(tag)) {
                    ArrayNode array = node.get(tag).isArray() ? (ArrayNode) node.get(tag) : mapper.createArrayNode().add(node.get(tag));
                    array.add(childJson);
                    node.set(tag, array);
                } else {
                    node.set(tag, childJson);
                }
            }
        }

        if (!hasElementChild) {
            String text = element.getTextContent().trim();
            if (!text.isEmpty()) return new TextNode(text);
        }

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            node.put(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
        }

        return node;
    }

    private static JsonNode nullIfMissing(JsonNode parent, String field) {
        JsonNode node = parent.path(field);
        return node.isMissingNode() ? NullNode.getInstance() : node;
    }

    private static String getNullableText(JsonNode node, String field) {
        String val = node.path(field).asText();
        return val.isEmpty() ? null : val;
    }

    private static JsonNode getNullableNode(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return (value.isMissingNode() || value.isObject() && value.size() == 0 || value.asText().isEmpty())
                ? NullNode.getInstance()
                : value.isTextual() ? new TextNode(value.asText()) : value;
    }
}
