package org.memmcol.gridflexbackendservice.doc;

public class OdysseyApiSample {

    public static final String METER_READINGS_200 = """
    {
      "pageLimit": 10,
      "total": 1,
      "readings": [
        {
          "meterId": "62525009163",
          "meterNumber": "62525009163",
          "energyConsumptionKwh": 79.65,
          "timeIntervalMinutes": 1440,
          "timestamp": "2026-05-08T00:00:00",
          "customerAccountId": "45900123276",
          "customerName": "John Doe",
          "meterState": "OFFLINE",
          "debt": 0,
          "credit": 0
        }
      ],
      "offset": 0,
      "errors": []
    }
    """;

    public static final String METER_READINGS_400 = """
     {
          "pageLimit": 0,
          "total": 0,
          "readings": [],
          "offset": 0,
          "errors": [
            "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ss"
          ]
     }
    """;

    public static final String PAYMENT_200 = """
    {
      "payments": [
        {
          "amount": 5000.00,
          "currency": "NGN",
          "transactionType": "FULL_PAYMENT",
          "transactionId": "RCPT-20260522103357-202006002221",
          "meterId": "202006002221",
          "timestamp": "2026-05-22T09:33:57.023Z",
          "customerId": "23ee204b8651",
          "customerAccountId": "0257946288",
          "customerName": "John Doe",
          "customerPhone": "08012345678",
          "latitude": 12.0,
          "longitude": 6543.0,
          "transactionKwh": 25.00
        }
      ],
      "errors": ""
    }
    """;

    public static final String PAYMENT_400 = """
    {
      "payments": [],
      "errors": "Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ss"
    }
    """;

    public static final String ERROR_400 = """
    {
      "responsecode": "100",
      "responsedesc": "Invalid request or date format",
      "responsedata": ""
    }
    """;

    public static final String ERROR_500 = """
    {
      "responsecode": "100",
      "responsedesc": "An unexpected error occurred",
      "responsedata": ""
    }
    """;
}
