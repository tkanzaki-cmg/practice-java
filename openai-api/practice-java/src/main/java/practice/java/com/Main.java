package practice.java.com;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import io.github.cdimascio.dotenv.Dotenv;

import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        // Load API key from .env file
        var dotenv = Dotenv.load();
        var apiKey = dotenv.get("API_KEY");
        var model = dotenv.get("MODEL", "gpt-3.5-turbo"); // デフォルトを指定

        // Hardcoded question texts
        var questionTexts = List.of(
            "SC10 あなたはオンラインクレーンゲームを知っていますか。（SA）",
            "SC3 最近、最も頻繁に使用しているSNSはどれですか。／Instagram(SA)",
            "SC15_2 あなたの趣味の中で、最も時間を費やしているものは何ですか。（MA）",
            "SC21 スポーツ観戦に興味がありますか。それとも、実際に参加することを好みますか。（SA）",
            "SC12_4 あなたがよく行く旅行先のタイプは何ですか。（例：温泉地、都市部など）（MA）",
            "SC8 最近読んだ本の中で特に印象に残ったジャンルを教えてください。（SA）",
            "SC18 映画館で映画を観る頻度はどのくらいですか。（例：週1回、月1回など）（SA）",
            "SC14_1 以下の楽器の中で演奏経験があるものを教えてください。／ピアノ(SA)",
            "SC20 動物を飼うことで得られる良い点は何だと思いますか。（MA）",
            "SC11_3 ゲームの中で最も好きなジャンルは何ですか。（例：RPG、シューティング、シミュレーション）（SA）"
        );

        try {
            // Read supporting files
            String goalText = readFileFromResources("ゴールと変数の定義.txt");
            String outputFormatText = readFileFromResources("出力形式.txt");
            String constraintsText = readFileFromResources("制約事項.txt");
            String prerequisitesText = readFileFromResources("前提条件.txt");
            String stepsText = readFileFromResources("手順と実行プロセス.txt");

            // Generate prompt text for all questions
            var requestText = String.format(
                    "以下の条件を元に複数の設問文からタイトルを抽出してください。\n\n" +
                            "ゴールと変数の定義:\n%s\n\n" +
                            "出力形式:\n%s\n\n" +
                            "制約事項:\n%s\n\n" +
                            "前提条件:\n%s\n\n" +
                            "手順と実行プロセス:\n%s\n\n" +
                            "設問文:\n%s",
                    goalText, outputFormatText, constraintsText, prerequisitesText, stepsText,
                    String.join("\n", questionTexts)
            );

            // Call OpenAI API
            List<String> response = extractTitlesUsingOpenAI(apiKey, model, requestText);

            // Print results
            System.out.println("プロンプト:\n" + requestText);
            System.out.println("抽出されたタイトル:");
            for (int i = 0; i < response.size(); i++) {
                System.out.printf("設問 %d: %s\n", i + 1, response.get(i));
            }

        } catch (IOException e) {
            System.err.println("エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String readFileFromResources(String fileName) throws IOException {
        var classLoader = Main.class.getClassLoader();
        var resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IOException("リソースファイルが見つかりません: " + fileName);
        }
        try {
            var filePath = Path.of(resource.toURI());
            return Files.lines(filePath).collect(Collectors.joining("\n"));
        } catch (URISyntaxException e) {
            throw new IOException("URI の形式が不正です: " + resource, e);
        }
    }

    private static List<String> extractTitlesUsingOpenAI(String apiKey, String model, String text) {
        try {
            var apiUrl = "https://api.openai.com/v1/chat/completions";

            // Create request body
            var requestBody = String.format(
                    """
                            {
                                "model": "%s",
                                "messages": [
                                    {"role": "system", "content": "You are a helpful assistant."},
                                    {"role": "user", "content": "%s"}
                                ],
                                "max_tokens": 500,
                                "temperature": 0.7
                            }
                            """,
                    model, text.replace("\n", "\\n"));

            // Open connection
            var connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send request body
            try (var os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            // Check response code
            var responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                try (var errorScanner = new Scanner(connection.getErrorStream(), StandardCharsets.UTF_8)) {
                    var errorBody = errorScanner.useDelimiter("\\A").next();
                    System.err.println("エラーレスポンス: " + errorBody);
                }
                throw new IOException("APIリクエストに失敗しました。ステータスコード: " + responseCode);
            }

            // Parse response
            try (var scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                var responseBody = scanner.useDelimiter("\\A").next();
                System.out.println("OpenAI APIレスポンス: " + responseBody);

                // Parse JSON and extract "content"
                var jsonResponse = new JSONObject(responseBody);
                var content = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                // Split the response into individual titles
                return List.of(content.split("\n"));
            }

        } catch (IOException e) {
            System.err.println("API呼び出しに失敗しました: " + e.getMessage());
            e.printStackTrace();
            return List.of("エラー: API呼び出しに失敗しました。");
        }
    }
}
