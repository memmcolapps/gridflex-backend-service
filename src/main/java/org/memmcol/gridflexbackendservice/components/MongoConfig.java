//package org.memmcol.gridflexbackendservice.components;
//
//
//import org.bson.json.Converter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
//import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
//
//import java.time.LocalDate;
//import java.time.ZoneOffset;
//import java.util.Arrays;
//import java.util.Date;
//
//@Configuration
//public class MongoConfig extends AbstractMongoClientConfiguration {
//
//    @Override
//    protected String getDatabaseName() {
//        return "yourDatabaseName";
//    }
//
//    @Bean
//    public MongoCustomConversions customConversions() {
//        return new MongoCustomConversions(Arrays.asList(
//                new Converter<LocalDate, Date>() {
//                    @Override
//                    public Date convert(LocalDate source) {
//                        return Date.from(source.atStartOfDay(ZoneOffset.UTC).toInstant());
//                    }
//                },
//                new Converter<Date, LocalDate>() {
//                    @Override
//                    public LocalDate convert(Date source) {
//                        return source.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
//                    }
//                }
//        ));
//    }
//}
//
