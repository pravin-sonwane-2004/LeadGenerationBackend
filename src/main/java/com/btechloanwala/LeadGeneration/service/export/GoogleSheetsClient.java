package com.btechloanwala.LeadGeneration.service.export;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Low-level client for the Google Sheets API.
 *
 * <p>Wraps the {@link Sheets} service account connection and exposes a single
 * {@link #appendRows(String, List)} operation that ALWAYS appends new rows below the
 * existing content of a tab — it never overwrites existing rows. The service account
 * credentials and the spreadsheet id come from configuration (env-var overridable).</p>
 *
 * <p>This class knows nothing about leads, entities, or the export workflow; it only
 * talks to the spreadsheet.</p>
 */
@Component
public class GoogleSheetsClient {

    private static final String APPEND_ANCHOR = "!A1";
    private static final String VALUE_INPUT_OPTION = "USER_ENTERED";
    private static final String INSERT_DATA_OPTION = "INSERT_ROWS";

    private final String credentialsFile;
    private final String spreadsheetId;

    public GoogleSheetsClient(
            @Value("${google.sheets.credentials.file}") String credentialsFile,
            @Value("${google.sheets.spreadsheet.id}") String spreadsheetId) {
        this.credentialsFile = credentialsFile;
        this.spreadsheetId = spreadsheetId;
    }

    /**
     * Appends {@code values} as new rows underneath the existing content of the given
     * tab.
     *
     * <p>Throws on any API failure (non-2xx response, network error, missing file).
     * Nothing is cached or committed by the caller until this method returns
     * successfully.</p>
     *
     * @param tabName name of the tab, e.g. {@code "Loan Applications"}
     * @param values  rows to append; each inner list is one spreadsheet row
     * @throws IOException                network/transport or API error
     * @throws java.security.GeneralSecurityException transport initialisation error
     * @throws IllegalStateException      credentials file missing / empty response
     */
    public void appendRows(String tabName, List<List<Object>> values)
            throws IOException, java.security.GeneralSecurityException {
        Sheets sheets = buildSheetsService();
        ValueRange body = new ValueRange().setValues(values);

        AppendValuesResponse response = sheets.spreadsheets().values()
                .append(spreadsheetId, tabName + APPEND_ANCHOR, body)
                .setValueInputOption(VALUE_INPUT_OPTION)
                .setInsertDataOption(INSERT_DATA_OPTION)
                .execute();

        if (response == null) {
            throw new IllegalStateException("Google Sheets API returned an empty response.");
        }
    }

    /**
     * Builds an authenticated {@link Sheets} service using the service-account JSON
     * credentials file configured in {@code google.sheets.credentials.file}.
     */
    private Sheets buildSheetsService() throws IOException, java.security.GeneralSecurityException {
        GoogleCredentials credentials;
        try (InputStream in = new FileInputStream(credentialsFile)) {
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
        }

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("LeadGeneration")
                .build();
    }
}
