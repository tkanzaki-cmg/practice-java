package practice.java.com;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        // .env からAPIキーを取得
        var dotenv = Dotenv.load();
        var api_key = dotenv.get("API_KEY");
        
        // タイトルを抽出するテキスト
        var text = "The Great Gatsby is a 1925 novel by American writer F. Scott Fitzgerald. Set in the Jazz Age on Long Island, near New York City, the novel depicts first-person narrator Nick Carraway's interactions with mysterious millionaire Jay Gatsby and Gatsby's obsession to reunite with his former lover, Daisy Buchanan.";
        var responce = extractTitleUsingOpenAI(api_key, text);
        System.out.println(responce);
    }
    
    private static String extractTitleUsingOpenAI(String apiKey, String text) {
        try {
            var openAiService = new OpenAiService(apiKey);
    
            var prompt = String.format("Extract the title of the following text:\n\n%s", text);
            var completionRequest = CompletionRequest.builder()
                    .prompt(prompt)
                    .model("babbage-002")
                    .maxTokens(50)
                    .temperature(0.7)
                    .build();
    
            var response = openAiService.createCompletion(completionRequest);
            return response.getChoices().get(0).getText().trim();
        } catch (Exception e) {
            System.err.println("API呼び出しに失敗しました: " + e.getMessage());
            e.printStackTrace();
            return "エラー: API呼び出しに失敗しました。";
        }
    }    
}
