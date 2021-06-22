package mySafestCountriesAPI.repos;

import mySafestCountriesAPI.beans.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRepo extends JpaRepository<Country, Integer> {

    List<Country> findByName(String name);
    List<Country> findByIso2(String iso2);

    boolean existsByName(String name);



}
