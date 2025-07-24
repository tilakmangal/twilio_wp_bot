package com.example.whatsappgpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import okhttp3.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@SpringBootApplication
public class WhatsAppGptApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhatsAppGptApplication.class, args);
    }
}

@RestController
@RequestMapping("/incoming")
class WhatsAppController {

    private static final String OPENAI_API_KEY = "sk-..."; // Replace with your key
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient();

    static {
        Twilio.init("TWILIO_SID", "TWILIO_AUTH_TOKEN"); // Replace with real values
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestParam Map<String, String> params) throws IOException {
        String from = params.get("From");
        String body = params.get("Body");

        String reply = getGeminiReply(body);

        Message.creator(
                new PhoneNumber(from),
                new PhoneNumber("whatsapp:+YOUR_TWILIO_NUMBER"),
                reply
        ).create();

        return ResponseEntity.ok("ok");
    }

    private String getGeminiReply(String userPrompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
    
        String geminiApiKey = "YOUR_GEMINI_API_KEY";
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + geminiApiKey;
    
        String requestJson = """
        {
          "contents": [{
            "parts": [{
              "text": "%s"
            }]
          }]
        }
        """.formatted(userPrompt.replace("\"", "\\\""));
    
        Request request = new Request.Builder()
                .url(geminiUrl)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();
    
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return "Gemini Error: " + response.message();
    
            String json = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> map = mapper.readValue(json, Map.class);
    
            return (String) ((Map<?, ?>)((Map<?, ?>)((java.util.List<?>) map.get("candidates")).get(0)).get("content")).get("parts").get(0).toString().replace("text=", "");
        }
    }

}
