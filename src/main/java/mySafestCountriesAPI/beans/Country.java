package mySafestCountriesAPI.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    @Id
    @GeneratedValue
    private int id;

    private String name;

    private long population;

    private String slug;

    private String iso2;

    private double avg;

    private double avg_div_pop;

    private String region;

    private String last_update;

    private String mako;
}
