package mySafestCountriesAPI.controllers;

import mySafestCountriesAPI.businessLogic.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("country")
public class CountryController {

    @Autowired
    CountryService countryService;


    @PostMapping("update-specific-country")
    public ResponseEntity<?> updateOne(String iso2){
        int id = countryService.updateOne(iso2);
        if (id == -1) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(HttpStatus.OK);
    }

//    @PostMapping("add-all")
//    public ResponseEntity<?> addAll() {
//        int id = countryService.addAll();
//        if (id == -1) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

    @PostMapping("update-all")
    public ResponseEntity<?> updateAll() throws IOException, InterruptedException {
        int id = countryService.updateAll();
        if (id == -1) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        return new ResponseEntity<>("updated successfully",HttpStatus.OK);
    }

    @PostMapping("update-remaining")
    public ResponseEntity<?> updateRemaining() throws IOException, InterruptedException {
        int id = countryService.updateRemaining();
        if (id == -1) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        return new ResponseEntity<>("updated successfully",HttpStatus.OK);
    }

//    @GetMapping("get-all")
//    public ResponseEntity<?> getAll(){
//        return new ResponseEntity<List<?>>(countryService.getAll(),HttpStatus.OK);
//    }

    @GetMapping("get-sorted")
    public ResponseEntity<?> getSorted(){
        return new ResponseEntity<List<?>>(countryService.getSorted(),HttpStatus.OK);
    }

//    @PostMapping("test")
//    public void test(){
//        countryService.test();
//    }
}
