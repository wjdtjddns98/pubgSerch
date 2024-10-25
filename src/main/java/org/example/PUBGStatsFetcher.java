package org.example;

import okhttp3.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Scanner;

public class PUBGStatsFetcher {

    private static final String API_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxNjJhOWUyMC03M2ZiLTAxM2QtZjgzZi01ZTQyYWFjM2UxNmUiLCJpc3MiOiJnYW1lbG9ja2VyIiwiaWF0IjoxNzI5NzQ5NTgyLCJwdWIiOiJibHVlaG9sZSIsInRpdGxlIjoicHViZyIsImFwcCI6ImZvcnN0dWR5In0.IkEfnFtr_mBky9XPA2ngIrbGn9zIotLXL9eNBjQQ1MQ";  // API 키 입력
    private static final String BASE_URL = "https://api.pubg.com/shards/";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean continueProgram = true;

        while (continueProgram) {
            printHeader("PUBG 전적 검색");

            String platform = selectPlatform(scanner);
            if (platform == null) continue;

            while (true) {
                String playerName = inputPlayerName(scanner);
                if (playerName == null) break;

                try {
                    String currentSeasonId = fetchCurrentSeasonId(platform);
                    if (currentSeasonId == null) {
                        printError("현재 시즌 ID를 가져올 수 없습니다.");
                        continue;
                    }

                    String playerId = fetchPlayerId(playerName, platform);
                    if (playerId != null) {
                        int modeChoice = selectMode(scanner);
                        if (modeChoice == -1) continue;

                        fetchStats(playerId, platform, currentSeasonId, modeChoice);
                    } else {
                        printError("플레이어를 찾을 수 없습니다.");
                    }
                } catch (IOException e) {
                    printError("데이터를 가져오는 중 오류가 발생했습니다.");
                    e.printStackTrace();
                }

                if (!askToContinue(scanner)) {
                    continueProgram = false;
                    break;
                }
            }
        }
        scanner.close();
    }

    private static String selectPlatform(Scanner scanner) {
        while (true) {
            printHeader("플랫폼 선택");
            System.out.println("1. 카카오");
            System.out.println("2. 스팀");
            System.out.println("3. 프로그램 종료");
            System.out.print("플랫폼을 선택하세요 (1, 2, 3): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": return "kakao";
                case "2": return "steam";
                case "3": System.exit(0);
                default: printError("잘못된 입력입니다. 다시 시도하세요.");
            }
        }
    }

    private static String inputPlayerName(Scanner scanner) {
        System.out.print("검색할 유저의 닉네임을 입력하세요 (뒤로 가려면 'b' 입력): ");
        String playerName = scanner.nextLine().trim();
        return playerName.equalsIgnoreCase("b") ? null : playerName;
    }

    private static int selectMode(Scanner scanner) {
        while (true) {
            printHeader("모드 선택");
            System.out.println("1. 일반게임");
            System.out.println("2. 경쟁전");
            System.out.println("3. 모두");
            System.out.println("4. 닉네임 입력으로 돌아가기");

            System.out.print("모드를 선택하세요 (1, 2, 3, 4): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": return 1;
                case "2": return 2;
                case "3": return 3;
                case "4": return -1;
                default: printError("잘못된 입력입니다. 다시 시도하세요.");
            }
        }
    }

    private static boolean askToContinue(Scanner scanner) {
        System.out.print("다른 유저를 검색하시겠습니까? (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }

    private static String fetchCurrentSeasonId(String platform) throws IOException {
        String url = BASE_URL + platform + "/seasons";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Accept", "application/vnd.api+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonArray seasons = JsonParser.parseString(response.body().string())
                        .getAsJsonObject().getAsJsonArray("data");

                for (JsonElement element : seasons) {
                    JsonObject season = element.getAsJsonObject();
                    if (season.getAsJsonObject("attributes").get("isCurrentSeason").getAsBoolean()) {
                        return season.get("id").getAsString();
                    }
                }
            }
        }
        return null;
    }

    private static String fetchPlayerId(String playerName, String platform) throws IOException {
        String url = BASE_URL + platform + "/players?filter[playerNames]=" + playerName;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Accept", "application/vnd.api+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return JsonParser.parseString(response.body().string())
                        .getAsJsonObject().getAsJsonArray("data")
                        .get(0).getAsJsonObject().get("id").getAsString();
            }
        }
        return null;
    }

    private static void fetchStats(String playerId, String platform, String seasonId, int modeChoice) throws IOException {
        if (modeChoice == 1 || modeChoice == 3) {
            fetchNormalStats(playerId, platform, seasonId);
        }
        if (modeChoice == 2 || modeChoice == 3) {
            fetchRankedStats(playerId, platform, seasonId, platform.equals("kakao"));
        }
    }

    private static void fetchNormalStats(String playerId, String platform, String seasonId) throws IOException {
        String url = BASE_URL + platform + "/players/" + playerId + "/seasons/" + seasonId;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Accept", "application/vnd.api+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject gameModes = JsonParser.parseString(response.body().string())
                        .getAsJsonObject().getAsJsonObject("data")
                        .getAsJsonObject("attributes").getAsJsonObject("gameModeStats");

                printStats("솔로", gameModes.getAsJsonObject("solo"));
                printStats("스쿼드", gameModes.getAsJsonObject("squad"));
            }
        }
    }

    private static void fetchRankedStats(String playerId, String platform, String seasonId, boolean excludeFpp) throws IOException {
        String url = BASE_URL + platform + "/players/" + playerId + "/seasons/" + seasonId + "/ranked";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Accept", "application/vnd.api+json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject rankedStats = JsonParser.parseString(response.body().string())
                        .getAsJsonObject().getAsJsonObject("data")
                        .getAsJsonObject("attributes").getAsJsonObject("rankedGameModeStats");

                printRankedStats("경쟁전 스쿼드", rankedStats.getAsJsonObject("squad"));
                if (!excludeFpp) {
                    printRankedStats("경쟁전 스쿼드 FPP", rankedStats.getAsJsonObject("squad-fpp"));
                }
            }
        }
    }

    private static void printStats(String mode, JsonObject stats) {
        if (stats == null || stats.get("roundsPlayed").getAsInt() == 0) {
            System.out.printf("[%s 모드] 전적 데이터가 없습니다.%n", mode);
            return;
        }

        int roundsPlayed = stats.get("roundsPlayed").getAsInt();
        double avgKills = stats.get("kills").getAsDouble() / roundsPlayed;
        double avgDamage = stats.get("damageDealt").getAsDouble() / roundsPlayed;

        System.out.printf("[%s 모드]%n", mode);
        System.out.printf("[게임 수] %d%n", roundsPlayed);
        System.out.printf("[K/D] %.2f%n", avgKills);
        System.out.printf("[평균 데미지] %.2f%n", avgDamage);
        System.out.println();
    }

    private static void printRankedStats(String mode, JsonObject stats) {
        if (stats == null || stats.get("roundsPlayed").getAsInt() == 0) {
            System.out.printf("[%s 모드] 전적 데이터가 없습니다.%n", mode);
            return;
        }

        String tier = stats.getAsJsonObject("currentTier").get("tier").getAsString();
        String subTier = stats.getAsJsonObject("currentTier").get("subTier").getAsString();
        int rankPoints = stats.get("currentRankPoint").getAsInt();
        double winRatio = stats.get("winRatio").getAsDouble() * 100;
        double top10Ratio = stats.get("top10Ratio").getAsDouble() * 100;

        System.out.printf("[%s 모드]%n", mode);
        System.out.printf("[티어] %s %s%n", tier, subTier);
        System.out.printf("[점수] %d RP%n", rankPoints);
        System.out.printf("[승률] %.2f%%%n", winRatio);
        System.out.printf("[탑 10 비율] %.2f%%%n", top10Ratio);
        System.out.println();
    }

    private static void printHeader(String title) {
        System.out.println("====================================");
        System.out.printf("            %s%n", title);
        System.out.println("====================================");
    }

    private static void printError(String message) {
        System.out.println("[오류] " + message);
    }
}
