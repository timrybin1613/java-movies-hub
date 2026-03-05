package ru.practicum.moviehub.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;

public final class GsonFactory {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .disableHtmlEscaping()
            .create();

    public static Gson get() {
        return GSON;
    }
}