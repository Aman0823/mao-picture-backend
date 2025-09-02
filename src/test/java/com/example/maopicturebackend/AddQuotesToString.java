package com.example.maopicturebackend;

public class AddQuotesToString {
    public static void main(String[] args) {
        String input = "S00625000001,S00345100001,S00329000001";
        String result = addQuotes(input);
        System.out.println(result);
    }

    public static String addQuotes(String input) {
        String[] parts = input.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = "'" + parts[i] + "'";
        }
        return String.join(",", parts);
    }
}
