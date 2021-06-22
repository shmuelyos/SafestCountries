package mySafestCountriesAPI.businessLogic;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mySafestCountriesAPI.beans.Country;
import mySafestCountriesAPI.repos.CountryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CountryService {


    @Autowired
    private CountryRepo countryRepo;

    private ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private String today_str;
    private String yesterday_str;
    private String _30days_ago_str;

    @PostConstruct
    public void init() {
        System.out.println("updating time");
        updateTime();
    }

    public void updateTime() {
        DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime _30_days_ago = today.minusDays(30);
        today_str = today.format(myFormat);
        yesterday_str = yesterday.format(myFormat);
        _30days_ago_str = _30_days_ago.format(myFormat);
    }

//    public Country2 countryBuilder(String iso2) throws IOException {
//
//        Country2 country2 = country2Repo.findByIso2(iso2).get(0);
//        if (country2 == null) return null;
//
//        URL url = new URL("https://api.covid19api.com/country/" + iso2 + "/status/confirmed?from=" + yesterday_str + "T00:00:00Z&to=" + yesterday_str + "T23:59:59Z");
//        URL url2 = new URL("https://api.covid19api.com/country/" + iso2 + "/status/confirmed?from=" + _30days_ago_str + "T00:00:00Z&to=" + _30days_ago_str + "T23:59:59Z");
//
//
//        List<fakeCountry> list = objectMapper.readValue(url, new TypeReference<>() {
//        });
//        List<fakeCountry> list2 = objectMapper.readValue(url2, new TypeReference<>() {
//        });
//
//
//        if (list.size() == 0 || list2.size() == 0) return null;
//
//        long yesterday_cases = getMaxCases(list);
//        long month_ago_cases = getMaxCases(list2);
//
//        double avg = yesterday_cases - month_ago_cases;
//        avg /= 30;
//        avg /= country2.getPopulation();
//
//        country2.setAvg_div_pop(avg);
//
//        return country2;
//
//    }


    public Country countryBuilder(String iso2) {
        if(iso2.equals("FR")) return fixFrance();
        String url = "https://api.covid19api.com/country/" + iso2 + "/status/confirmed?from=" + yesterday_str + "T00:00:00Z&to=" + yesterday_str + "T23:59:59Z";
        String url2 = "https://api.covid19api.com/country/" + iso2 + "/status/confirmed?from=" + _30days_ago_str + "T00:00:00Z&to=" + _30days_ago_str + "T23:59:59Z";

        Country country = countryRepo.findByIso2(iso2).get(0);
        if (country == null) return null;

        List<HashMap<String, Object>> list = restTemplate.getForObject(url, List.class);
        List<HashMap<String, Object>> list2 = restTemplate.getForObject(url2, List.class);

        if (list.size() == 0 || list2.size() == 0) return null;

        int yesterday_cases = getCorrectCases(list);
        int month_ago_cases = getCorrectCases(list2);

        double avg = yesterday_cases - month_ago_cases;
        avg /= 30;
        country.setAvg(avg);

        avg /= country.getPopulation();
        country.setAvg_div_pop(avg);
        country.setLast_update(today_str);
//        System.out.println("Country name = "+country.getName() +", yesterday cases - month ago cases = "+yesterday_cases +" - "+month_ago_cases+" .");
//        System.out.println("avg = "+avg +", avg/pop = "+avg/country.getPopulation()+" ." );
//        System.out.println("yesterday date = "+yesterday_str+" , month ago date = "+_30days_ago_str);

        return country;

    }

    private Country fixFrance() {
        List<Country> list = countryRepo.findByIso2("FR");
        if(list.size()==0)return null;
        Country france = list.get(0);
        double yesterday_france_cases = 5685915;
        double monthAgo_france_cases = 5330707;
        double avg = yesterday_france_cases - monthAgo_france_cases / 30;
        france.setAvg(avg);
        france.setAvg_div_pop(avg/france.getPopulation());
        france.setLast_update(today_str);
        return france;
    }

    private int getCorrectCases(List<HashMap<String, Object>> list) {
        int count=0;
        for (HashMap <String,Object> m: list
             ) {
            if(m.get("Province")=="") return (int)m.get("Cases");
            count += (int) m.get("Cases");
        }
        return count;
    }

//    private int getSum(List<HashMap<String, Object>> list) {
//        int count = 0;
//        for (HashMap<String, Object> m : list) {
//            count += (int) m.get("Cases");
//        }
//        return count;
//    }
//
//    private int getCases(List<HashMap<String, Object>> list) {
//        List<Integer> list_cases = new ArrayList<>();
//        for (HashMap<String, Object> m : list) {
//            int cases = (int) m.get("Cases");
//            list_cases.add(cases);
//        }
//        return Collections.max(list_cases);
//    }

//    private long getMaxCases(List<fakeCountry> list) {
//        long max = 0;
//        for (fakeCountry f : list
//        ) {
//            if (f.getCases() > max) max = f.getCases();
//        }
//        return max;
//    }


    public int addAll() {
        try {
            List<Country> countryList = objectMapper.readValue(getStr(), new TypeReference<List<Country>>() {
            });
            if (countryList.size() == 0) return -1;
            countryRepo.saveAll(countryList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return 1;
    }


    public int updateAll() throws IOException, InterruptedException {
        updateTime();
        int i = 0;
        for (Country c : countryRepo.findAll()) {
            if(c.getLast_update()==null) continue;

            System.out.println("trying to update "+c.getName());
            i = updateOne(c.getIso2());
            if (i == -1) System.out.println("unable to update : " + c.getName());

            Thread.sleep(1000);

        }
        System.out.println("finished");
        return 1;
    }

    public int updateRemaining() throws IOException, InterruptedException {
        updateTime();
        int i = 0;
        for (Country c : countryRepo.findAll()) {
            if(c.getLast_update()==null) continue;
            if (!c.getLast_update().equals(today_str) ) {
                System.out.println("trying to update "+c.getName());
                i = updateOne(c.getIso2());
                if (i == -1) System.out.println("unable to update : " + c.getName());
                Thread.sleep(1000);
            }
        }
        System.out.println("finished");
        return 1;
    }


    public int updateOne(String iso2) {
        Country c = countryBuilder(iso2);
        if (c == null) return -1;
        countryRepo.save(c);
        return 1;
    }

    public String getStr() {
        return "[\n" +
                "    {\"name\": \"Burkina Faso\", \"slug\": \"burkina-faso\", \"iso2\": \"BF\", \"population\": 19751535}\n" +
                "    ,\n" +
                "    {\"name\": \"Hong Kong, SAR China\", \"slug\": \"hong-kong-sar-china\", \"iso2\": \"HK\", \"population\": 7436154}\n" +
                "    ,\n" +
                "    {\"name\": \"Syrian Arab Republic (Syria)\", \"slug\": \"syria\", \"iso2\": \"SY\", \"population\": 17500658}\n" +
                "    ,\n" +
                "    {\"name\": \"Egypt\", \"slug\": \"egypt\", \"iso2\": \"EG\", \"population\": 98423595}\n" +
                "    ,\n" +
                "    {\"name\": \"Israel\", \"slug\": \"israel\", \"iso2\": \"IL\", \"population\": 9291000}\n" +
                "    ,\n" +
                "    {\"name\": \"Palestinian Territory\", \"slug\": \"palestine\", \"iso2\": \"PS\", \"population\": 5101414}\n" +
                "    ,\n" +
                "    {\"name\": \"Algeria\", \"slug\": \"algeria\", \"iso2\": \"DZ\", \"population\": 42228429}\n" +
                "    ,\n" +
                "    {\"name\": \"Azerbaijan\", \"slug\": \"azerbaijan\", \"iso2\": \"AZ\", \"population\": 9939800}\n" +
                "    ,\n" +
                "    {\"name\": \"Comoros\", \"slug\": \"comoros\", \"iso2\": \"KM\", \"population\": 832322}\n" +
                "    ,\n" +
                "    {\"name\": \"Netherlands\", \"slug\": \"netherlands\", \"iso2\": \"NL\", \"population\": 17231624}\n" +
                "    ,\n" +
                "    {\"name\": \"Philippines\", \"slug\": \"philippines\", \"iso2\": \"PH\", \"population\": 106651922}\n" +
                "    ,\n" +
                "    {\"name\": \"Russian Federation\", \"slug\": \"russia\", \"iso2\": \"RU\", \"population\": 144478050}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint Vincent and Grenadines\", \"slug\": \"saint-vincent-and-the-grenadines\", \"iso2\": \"VC\",\n" +
                "     \"population\": 111258}\n" +
                "    ,\n" +
                "    {\"name\": \"Iraq\", \"slug\": \"iraq\", \"iso2\": \"IQ\", \"population\": 38433600}\n" +
                "    ,\n" +
                "    {\"name\": \"Monaco\", \"slug\": \"monaco\", \"iso2\": \"MC\", \"population\": 38682}\n" +
                "    ,\n" +
                "    {\"name\": \"Qatar\", \"slug\": \"qatar\", \"iso2\": \"QA\", \"population\": 2781677}\n" +
                "    ,\n" +
                "    {\"name\": \"Afghanistan\", \"slug\": \"afghanistan\", \"iso2\": \"AF\", \"population\": 37172386}\n" +
                "    ,\n" +
                "    {\"name\": \"Burundi\", \"slug\": \"burundi\", \"iso2\": \"BI\", \"population\": 11175378}\n" +
                "    ,\n" +
                "    {\"name\": \"Cocos (Keeling) Islands\", \"slug\": \"cocos-keeling-islands\", \"iso2\": \"CC\", \"population\": 596}\n" +
                "    ,\n" +
                "    {\"name\": \"Mali\", \"slug\": \"mali\", \"iso2\": \"ML\", \"population\": 19077690}\n" +
                "    ,\n" +
                "    {\"name\": \"ALA Aland Islands\", \"slug\": \"ala-aland-islands\", \"iso2\": \"AX\", \"population\": 6500}\n" +
                "    ,\n" +
                "    {\"name\": \"Finland\", \"slug\": \"finland\", \"iso2\": \"FI\", \"population\": 5515525}\n" +
                "    ,\n" +
                "    {\"name\": \"Puerto Rico\", \"slug\": \"puerto-rico\", \"iso2\": \"PR\", \"population\": 3195153}\n" +
                "    ,\n" +
                "    {\"name\": \"Singapore\", \"slug\": \"singapore\", \"iso2\": \"SG\", \"population\": 5638676}\n" +
                "    ,\n" +
                "    {\"name\": \"Honduras\", \"slug\": \"honduras\", \"iso2\": \"HN\", \"population\": 9587522}\n" +
                "    ,\n" +
                "    {\"name\": \"Kuwait\", \"slug\": \"kuwait\", \"iso2\": \"KW\", \"population\": 4137309}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint Kitts and Nevis\", \"slug\": \"saint-kitts-and-nevis\", \"iso2\": \"KN\", \"population\": 52441}\n" +
                "    ,\n" +
                "    {\"name\": \"Zimbabwe\", \"slug\": \"zimbabwe\", \"iso2\": \"ZW\", \"population\": 14439018}\n" +
                "    ,\n" +
                "    {\"name\": \"Bahamas\", \"slug\": \"bahamas\", \"iso2\": \"BS\", \"population\": 385640}\n" +
                "    ,\n" +
                "    {\"name\": \"Macedonia, Republic of\", \"slug\": \"macedonia\", \"iso2\": \"MK\", \"population\": 2086648}\n" +
                "    ,\n" +
                "    {\"name\": \"New Caledonia\", \"slug\": \"new-caledonia\", \"iso2\": \"NC\", \"population\": 284060}\n" +
                "    ,\n" +
                "    {\"name\": \"Turks and Caicos Islands\", \"slug\": \"turks-and-caicos-islands\", \"iso2\": \"TC\", \"population\": 37665}\n" +
                "    ,\n" +
                "    {\"name\": \"Cuba\", \"slug\": \"cuba\", \"iso2\": \"CU\", \"population\": 11338138}\n" +
                "    ,\n" +
                "    {\"name\": \"Haiti\", \"slug\": \"haiti\", \"iso2\": \"HT\", \"population\": 11123176}\n" +
                "    ,\n" +
                "    {\"name\": \"Mongolia\", \"slug\": \"mongolia\", \"iso2\": \"MN\", \"population\": 3170208}\n" +
                "    ,\n" +
                "    {\"name\": \"Tajikistan\", \"slug\": \"tajikistan\", \"iso2\": \"TJ\", \"population\": 9100837}\n" +
                "    ,\n" +
                "    {\"name\": \"Guinea-Bissau\", \"slug\": \"guinea-bissau\", \"iso2\": \"GW\", \"population\": 1874309}\n" +
                "    ,\n" +
                "    {\"name\": \"Montserrat\", \"slug\": \"montserrat\", \"iso2\": \"MS\", \"population\": 5900}\n" +
                "    ,\n" +
                "    {\"name\": \"Norway\", \"slug\": \"norway\", \"iso2\": \"NO\", \"population\": 5311916}\n" +
                "    ,\n" +
                "    {\"name\": \"Liberia\", \"slug\": \"liberia\", \"iso2\": \"LR\", \"population\": 4818977}\n" +
                "    ,\n" +
                "    {\"name\": \"Mayotte\", \"slug\": \"mayotte\", \"iso2\": \"YT\", \"population\": 270372}\n" +
                "    ,\n" +
                "    {\"name\": \"Papua New Guinea\", \"slug\": \"papua-new-guinea\", \"iso2\": \"PG\", \"population\": 8606316}\n" +
                "    ,\n" +
                "    {\"name\": \"Timor-Leste\", \"slug\": \"timor-leste\", \"iso2\": \"TL\", \"population\": 1341015}\n" +
                "    ,\n" +
                "    {\"name\": \"Anguilla\", \"slug\": \"anguilla\", \"iso2\": \"AI\", \"population\": 15094}\n" +
                "    ,\n" +
                "    {\"name\": \"Australia\", \"slug\": \"australia\", \"iso2\": \"AU\", \"population\": 24982688}\n" +
                "    ,\n" +
                "    {\"name\": \"Gambia\", \"slug\": \"gambia\", \"iso2\": \"GM\", \"population\": 2280102}\n" +
                "    ,\n" +
                "    {\"name\": \"Belarus\", \"slug\": \"belarus\", \"iso2\": \"BY\", \"population\": 9483499}\n" +
                "    ,\n" +
                "    {\"name\": \"Svalbard and Jan Mayen Islands\", \"slug\": \"svalbard-and-jan-mayen-islands\", \"iso2\": \"SJ\",\n" +
                "     \"population\": 2939}\n" +
                "    ,\n" +
                "    {\"name\": \"Andorra\", \"slug\": \"andorra\", \"iso2\": \"AD\", \"population\": 77006}\n" +
                "    ,\n" +
                "    {\"name\": \"Cambodia\", \"slug\": \"cambodia\", \"iso2\": \"KH\", \"population\": 16249798}\n" +
                "    ,\n" +
                "    {\"name\": \"France\", \"slug\": \"france\", \"iso2\": \"FR\", \"population\": 66977107}\n" +
                "    ,\n" +
                "    {\"name\": \"Cayman Islands\", \"slug\": \"cayman-islands\", \"iso2\": \"KY\", \"population\": 64174}\n" +
                "    ,\n" +
                "    {\"name\": \"Macao, SAR China\", \"slug\": \"macao-sar-china\", \"iso2\": \"MO\", \"population\": 649335}\n" +
                "    ,\n" +
                "    {\"name\": \"United States of America\", \"slug\": \"united-states\", \"iso2\": \"US\", \"population\": 332759663}\n" +
                "    ,\n" +
                "    {\"name\": \"China\", \"slug\": \"china\", \"iso2\": \"CN\", \"population\": 1392730000}\n" +
                "    ,\n" +
                "    {\"name\": \"Italy\", \"slug\": \"italy\", \"iso2\": \"IT\", \"population\": 60421760}\n" +
                "    ,\n" +
                "    {\"name\": \"Liechtenstein\", \"slug\": \"liechtenstein\", \"iso2\": \"LI\", \"population\": 37910}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint Lucia\", \"slug\": \"saint-lucia\", \"iso2\": \"LC\", \"population\": 181889}\n" +
                "    ,\n" +
                "    {\"name\": \"British Indian Ocean Territory\", \"slug\": \"british-indian-ocean-territory\", \"iso2\": \"IO\",\n" +
                "     \"population\": 4000}\n" +
                "    ,\n" +
                "    {\"name\": \"Guyana\", \"slug\": \"guyana\", \"iso2\": \"GY\", \"population\": 779004}\n" +
                "    ,\n" +
                "    {\"name\": \"Sierra Leone\", \"slug\": \"sierra-leone\", \"iso2\": \"SL\", \"population\": 7650154}\n" +
                "    ,\n" +
                "    {\"name\": \"Venezuela (Bolivarian Republic)\", \"slug\": \"venezuela\", \"iso2\": \"VE\", \"population\": 28363280}\n" +
                "    ,\n" +
                "    {\"name\": \"Madagascar\", \"slug\": \"madagascar\", \"iso2\": \"MG\", \"population\": 26262368}\n" +
                "    ,\n" +
                "    {\"name\": \"El Salvador\", \"slug\": \"el-salvador\", \"iso2\": \"SV\", \"population\": 6420744}\n" +
                "    ,\n" +
                "    {\"name\": \"Ghana\", \"slug\": \"ghana\", \"iso2\": \"GH\", \"population\": 29767108}\n" +
                "    ,\n" +
                "    {\"name\": \"Hungary\", \"slug\": \"hungary\", \"iso2\": \"HU\", \"population\": 9775564}\n" +
                "    ,\n" +
                "    {\"name\": \"Micronesia, Federated States of\", \"slug\": \"micronesia\", \"iso2\": \"FM\", \"population\": 112640}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint Helena\", \"slug\": \"saint-helena\", \"iso2\": \"SH\", \"population\": 6600}\n" +
                "    ,\n" +
                "    {\"name\": \"Republic of Kosovo\", \"slug\": \"kosovo\", \"iso2\": \"XK\", \"population\": 1768172}\n" +
                "    ,\n" +
                "    {\"name\": \"Viet Nam\", \"slug\": \"vietnam\", \"iso2\": \"VN\", \"population\": 98168833}\n" +
                "    ,\n" +
                "    {\"name\": \"Bolivia\", \"slug\": \"bolivia\", \"iso2\": \"BO\", \"population\": 11353142}\n" +
                "    ,\n" +
                "    {\"name\": \"Gibraltar\", \"slug\": \"gibraltar\", \"iso2\": \"GI\", \"population\": 33718}\n" +
                "    ,\n" +
                "    {\"name\": \"Kyrgyzstan\", \"slug\": \"kyrgyzstan\", \"iso2\": \"KG\", \"population\": 6322800}\n" +
                "    ,\n" +
                "    {\"name\": \"Malawi\", \"slug\": \"malawi\", \"iso2\": \"MW\", \"population\": 18143315}\n" +
                "    ,\n" +
                "    {\"name\": \"US Minor Outlying Islands\", \"slug\": \"us-minor-outlying-islands\", \"iso2\": \"UM\", \"population\": 300}\n" +
                "    ,\n" +
                "    {\"name\": \"Malta\", \"slug\": \"malta\", \"iso2\": \"MT\", \"population\": 484630}\n" +
                "    ,\n" +
                "    {\"name\": \"Nepal\", \"slug\": \"nepal\", \"iso2\": \"NP\", \"population\": 28087871}\n" +
                "    ,\n" +
                "    {\"name\": \"New Zealand\", \"slug\": \"new-zealand\", \"iso2\": \"NZ\", \"population\": 4841000}\n" +
                "    ,\n" +
                "    {\"name\": \"Sudan\", \"slug\": \"sudan\", \"iso2\": \"SD\", \"population\": 41801533}\n" +
                "    ,\n" +
                "    {\"name\": \"Tunisia\", \"slug\": \"tunisia\", \"iso2\": \"TN\", \"population\": 11565204}\n" +
                "    ,\n" +
                "    {\"name\": \"Spain\", \"slug\": \"spain\", \"iso2\": \"ES\", \"population\": 46796540}\n" +
                "    ,\n" +
                "    {\"name\": \"Angola\", \"slug\": \"angola\", \"iso2\": \"AO\", \"population\": 30809762}\n" +
                "    ,\n" +
                "    {\"name\": \"Belgium\", \"slug\": \"belgium\", \"iso2\": \"BE\", \"population\": 11433256}\n" +
                "    ,\n" +
                "    {\"name\": \"Bhutan\", \"slug\": \"bhutan\", \"iso2\": \"BT\", \"population\": 754394}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint-Martin (French part)\", \"slug\": \"saint-martin-french-part\", \"iso2\": \"MF\", \"population\": 39255}\n" +
                "    ,\n" +
                "    {\"name\": \"Zambia\", \"slug\": \"zambia\", \"iso2\": \"ZM\", \"population\": 17351822}\n" +
                "    ,\n" +
                "    {\"name\": \"Brunei Darussalam\", \"slug\": \"brunei\", \"iso2\": \"BN\", \"population\": 437479}\n" +
                "    ,\n" +
                "    {\"name\": \"Cameroon\", \"slug\": \"cameroon\", \"iso2\": \"CM\", \"population\": 25216237}\n" +
                "    ,\n" +
                "    {\"name\": \"Poland\", \"slug\": \"poland\", \"iso2\": \"PL\", \"population\": 37974750}\n" +
                "    ,\n" +
                "    {\"name\": \"Tuvalu\", \"slug\": \"tuvalu\", \"iso2\": \"TV\", \"population\": 11508}\n" +
                "    ,\n" +
                "    {\"name\": \"Central African Republic\", \"slug\": \"central-african-republic\", \"iso2\": \"CF\", \"population\": 4666377}\n" +
                "    ,\n" +
                "    {\"name\": \"Djibouti\", \"slug\": \"djibouti\", \"iso2\": \"DJ\", \"population\": 958920}\n" +
                "    ,\n" +
                "    {\"name\": \"Marshall Islands\", \"slug\": \"marshall-islands\", \"iso2\": \"MH\", \"population\": 58413}\n" +
                "    ,\n" +
                "    {\"name\": \"Sao Tome and Principe\", \"slug\": \"sao-tome-and-principe\", \"iso2\": \"ST\", \"population\": 211028}\n" +
                "    ,\n" +
                "    {\"name\": \"Sweden\", \"slug\": \"sweden\", \"iso2\": \"SE\", \"population\": 10175214}\n" +
                "    ,\n" +
                "    {\"name\": \"Tanzania, United Republic of\", \"slug\": \"tanzania\", \"iso2\": \"TZ\", \"population\": 59734218}\n" +
                "    ,\n" +
                "    {\"name\": \"Western Sahara\", \"slug\": \"western-sahara\", \"iso2\": \"EH\", \"population\": 652271}\n" +
                "    ,\n" +
                "    {\"name\": \"Iceland\", \"slug\": \"iceland\", \"iso2\": \"IS\", \"population\": 352721}\n" +
                "    ,\n" +
                "    {\"name\": \"Namibia\", \"slug\": \"namibia\", \"iso2\": \"NA\", \"population\": 2448255}\n" +
                "    ,\n" +
                "    {\"name\": \"Vanuatu\", \"slug\": \"vanuatu\", \"iso2\": \"VU\", \"population\": 292680}\n" +
                "    ,\n" +
                "    {\"name\": \"Yemen\", \"slug\": \"yemen\", \"iso2\": \"YE\", \"population\": 28498687}\n" +
                "    ,\n" +
                "    {\"name\": \"Bangladesh\", \"slug\": \"bangladesh\", \"iso2\": \"BD\", \"population\": 161356039}\n" +
                "    ,\n" +
                "    {\"name\": \"Canada\", \"slug\": \"canada\", \"iso2\": \"CA\", \"population\": 37057765}\n" +
                "    ,\n" +
                "    {\"name\": \"French Southern Territories\", \"slug\": \"french-southern-territories\", \"iso2\": \"TF\", \"population\": 401}\n" +
                "    ,\n" +
                "    {\"name\": \"Ireland\", \"slug\": \"ireland\", \"iso2\": \"IE\", \"population\": 4867309}\n" +
                "    ,\n" +
                "    {\"name\": \"San Marino\", \"slug\": \"san-marino\", \"iso2\": \"SM\", \"population\": 33785}\n" +
                "    ,\n" +
                "    {\"name\": \"Tokelau\", \"slug\": \"tokelau\", \"iso2\": \"TK\", \"population\": 1411}\n" +
                "    ,\n" +
                "    {\"name\": \"Bosnia and Herzegovina\", \"slug\": \"bosnia-and-herzegovina\", \"iso2\": \"BA\", \"population\": 3323929}\n" +
                "    ,\n" +
                "    {\"name\": \"Indonesia\", \"slug\": \"indonesia\", \"iso2\": \"ID\", \"population\": 267663435}\n" +
                "    ,\n" +
                "    {\"name\": \"Mexico\", \"slug\": \"mexico\", \"iso2\": \"MX\", \"population\": 126190788}\n" +
                "    ,\n" +
                "    {\"name\": \"Lebanon\", \"slug\": \"lebanon\", \"iso2\": \"LB\", \"population\": 6848925}\n" +
                "    ,\n" +
                "    {\"name\": \"Pakistan\", \"slug\": \"pakistan\", \"iso2\": \"PK\", \"population\": 212215030}\n" +
                "    ,\n" +
                "    {\"name\": \"Uruguay\", \"slug\": \"uruguay\", \"iso2\": \"UY\", \"population\": 3449299}\n" +
                "    ,\n" +
                "    {\"name\": \"Holy See (Vatican City State)\", \"slug\": \"holy-see-vatican-city-state\", \"iso2\": \"VA\", \"population\": 825}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint-Barthélemy\", \"slug\": \"saint-barthélemy\", \"iso2\": \"BL\", \"population\": 9904}\n" +
                "    ,\n" +
                "    {\"name\": \"Serbia\", \"slug\": \"serbia\", \"iso2\": \"RS\", \"population\": 6963764}\n" +
                "    ,\n" +
                "    {\"name\": \"French Polynesia\", \"slug\": \"french-polynesia\", \"iso2\": \"PF\", \"population\": 277679}\n" +
                "    ,\n" +
                "    {\"name\": \"Kiribati\", \"slug\": \"kiribati\", \"iso2\": \"KI\", \"population\": 115847}\n" +
                "    ,\n" +
                "    {\"name\": \"Malaysia\", \"slug\": \"malaysia\", \"iso2\": \"MY\", \"population\": 31528585}\n" +
                "    ,\n" +
                "    {\"name\": \"Morocco\", \"slug\": \"morocco\", \"iso2\": \"MA\", \"population\": 36029138}\n" +
                "    ,\n" +
                "    {\"name\": \"Niue\", \"slug\": \"niue\", \"iso2\": \"NU\", \"population\": 1624}\n" +
                "    ,\n" +
                "    {\"name\": \"Oman\", \"slug\": \"oman\", \"iso2\": \"OM\", \"population\": 4829483}\n" +
                "    ,\n" +
                "    {\"name\": \"Christmas Island\", \"slug\": \"christmas-island\", \"iso2\": \"CX\", \"population\": 1402}\n" +
                "    ,\n" +
                "    {\"name\": \"Cyprus\", \"slug\": \"cyprus\", \"iso2\": \"CY\", \"population\": 1189265}\n" +
                "    ,\n" +
                "    {\"name\": \"Japan\", \"slug\": \"japan\", \"iso2\": \"JP\", \"population\": 126529100}\n" +
                "    ,\n" +
                "    {\"name\": \"Taiwan, Republic of China\", \"slug\": \"taiwan\", \"iso2\": \"TW\", \"population\": 23855722}\n" +
                "    ,\n" +
                "    {\"name\": \"Croatia\", \"slug\": \"croatia\", \"iso2\": \"HR\", \"population\": 4087843}\n" +
                "    ,\n" +
                "    {\"name\": \"Guatemala\", \"slug\": \"guatemala\", \"iso2\": \"GT\", \"population\": 17247807}\n" +
                "    ,\n" +
                "    {\"name\": \"Mauritania\", \"slug\": \"mauritania\", \"iso2\": \"MR\", \"population\": 4403319}\n" +
                "    ,\n" +
                "    {\"name\": \"Congo (Kinshasa)\", \"slug\": \"congo-kinshasa\", \"iso2\": \"CD\", \"population\": 14342000}\n" +
                "    ,\n" +
                "    {\"name\": \"Denmark\", \"slug\": \"denmark\", \"iso2\": \"DK\", \"population\": 5793636}\n" +
                "    ,\n" +
                "    {\"name\": \"Senegal\", \"slug\": \"senegal\", \"iso2\": \"SN\", \"population\": 15854360}\n" +
                "    ,\n" +
                "    {\"name\": \"Jamaica\", \"slug\": \"jamaica\", \"iso2\": \"JM\", \"population\": 2934855}\n" +
                "    ,\n" +
                "    {\"name\": \"Uganda\", \"slug\": \"uganda\", \"iso2\": \"UG\", \"population\": 42723139}\n" +
                "    ,\n" +
                "    {\"name\": \"Bahrain\", \"slug\": \"bahrain\", \"iso2\": \"BH\", \"population\": 1569439}\n" +
                "    ,\n" +
                "    {\"name\": \"Chile\", \"slug\": \"chile\", \"iso2\": \"CL\", \"population\": 18729160}\n" +
                "    ,\n" +
                "    {\"name\": \"Estonia\", \"slug\": \"estonia\", \"iso2\": \"EE\", \"population\": 1321977}\n" +
                "    ,\n" +
                "    {\"name\": \"Kazakhstan\", \"slug\": \"kazakhstan\", \"iso2\": \"KZ\", \"population\": 18272430}\n" +
                "    ,\n" +
                "    {\"name\": \"Mozambique\", \"slug\": \"mozambique\", \"iso2\": \"MZ\", \"population\": 29495962}\n" +
                "    ,\n" +
                "    {\"name\": \"Netherlands Antilles\", \"slug\": \"netherlands-antilles\", \"iso2\": \"AN\", \"population\": 227049}\n" +
                "    ,\n" +
                "    {\"name\": \"Dominican Republic\", \"slug\": \"dominican-republic\", \"iso2\": \"DO\", \"population\": 10627165}\n" +
                "    ,\n" +
                "    {\"name\": \"Gabon\", \"slug\": \"gabon\", \"iso2\": \"GA\", \"population\": 2119275}\n" +
                "    ,\n" +
                "    {\"name\": \"Iran, Islamic Republic of\", \"slug\": \"iran\", \"iso2\": \"IR\", \"population\": 85028759}\n" +
                "    ,\n" +
                "    {\"name\": \"Lao PDR\", \"slug\": \"lao-pdr\", \"iso2\": \"LA\", \"population\": 7370880}\n" +
                "    ,\n" +
                "    {\"name\": \"Maldives\", \"slug\": \"maldives\", \"iso2\": \"MV\", \"population\": 515696}\n" +
                "    ,\n" +
                "    {\"name\": \"South Georgia and the South Sandwich Islands\", \"slug\": \"south-georgia-and-the-south-sandwich-islands\",\n" +
                "     \"iso2\": \"GS\", \"population\": 30}\n" +
                "    ,\n" +
                "    {\"name\": \"Guernsey\", \"slug\": \"guernsey\", \"iso2\": \"GG\", \"population\": 63000}\n" +
                "    ,\n" +
                "    {\"name\": \"Rwanda\", \"slug\": \"rwanda\", \"iso2\": \"RW\", \"population\": 12301939}\n" +
                "    ,\n" +
                "    {\"name\": \"Greece\", \"slug\": \"greece\", \"iso2\": \"GR\", \"population\": 10731726}\n" +
                "    ,\n" +
                "    {\"name\": \"Trinidad and Tobago\", \"slug\": \"trinidad-and-tobago\", \"iso2\": \"TT\", \"population\": 1389858}\n" +
                "    ,\n" +
                "    {\"name\": \"Switzerland\", \"slug\": \"switzerland\", \"iso2\": \"CH\", \"population\": 8513227}\n" +
                "    ,\n" +
                "    {\"name\": \"Cook Islands\", \"slug\": \"cook-islands\", \"iso2\": \"CK\", \"population\": 17379}\n" +
                "    ,\n" +
                "    {\"name\": \"India\", \"slug\": \"india\", \"iso2\": \"IN\", \"population\": 1352617328}\n" +
                "    ,\n" +
                "    {\"name\": \"Myanmar\", \"slug\": \"myanmar\", \"iso2\": \"MM\", \"population\": 53708395}\n" +
                "    ,\n" +
                "    {\"name\": \"Romania\", \"slug\": \"romania\", \"iso2\": \"RO\", \"population\": 19466145}\n" +
                "    ,\n" +
                "    {\"name\": \"Seychelles\", \"slug\": \"seychelles\", \"iso2\": \"SC\", \"population\": 96762}\n" +
                "    ,\n" +
                "    {\"name\": \"Virgin Islands, US\", \"slug\": \"virgin-islands\", \"iso2\": \"VI\", \"population\": 104286}\n" +
                "    ,\n" +
                "    {\"name\": \"Belize\", \"slug\": \"belize\", \"iso2\": \"BZ\", \"population\": 383071}\n" +
                "    ,\n" +
                "    {\"name\": \"Côte d'Ivoire\", \"slug\": \"cote-divoire\", \"iso2\": \"CI\", \"population\": 26966026}\n" +
                "    ,\n" +
                "    {\"name\": \"Faroe Islands\", \"slug\": \"faroe-islands\", \"iso2\": \"FO\", \"population\": 48497}\n" +
                "    ,\n" +
                "    {\"name\": \"Martinique\", \"slug\": \"martinique\", \"iso2\": \"MQ\", \"population\": 376480}\n" +
                "    ,\n" +
                "    {\"name\": \"Northern Mariana Islands\", \"slug\": \"northern-mariana-islands\", \"iso2\": \"MP\", \"population\": 56882}\n" +
                "    ,\n" +
                "    {\"name\": \"Heard and Mcdonald Islands\", \"slug\": \"heard-and-mcdonald-islands\", \"iso2\": \"HM\", \"population\": 0}\n" +
                "    ,\n" +
                "    {\"name\": \"Luxembourg\", \"slug\": \"luxembourg\", \"iso2\": \"LU\", \"population\": 607950}\n" +
                "    ,\n" +
                "    {\"name\": \"South Africa\", \"slug\": \"south-africa\", \"iso2\": \"ZA\", \"population\": 57779622}\n" +
                "    ,\n" +
                "    {\"name\": \"Armenia\", \"slug\": \"armenia\", \"iso2\": \"AM\", \"population\": 2951776}\n" +
                "    ,\n" +
                "    {\"name\": \"Costa Rica\", \"slug\": \"costa-rica\", \"iso2\": \"CR\", \"population\": 4999441}\n" +
                "    ,\n" +
                "    {\"name\": \"Turkmenistan\", \"slug\": \"turkmenistan\", \"iso2\": \"TM\", \"population\": 5850908}\n" +
                "    ,\n" +
                "    {\"name\": \"Antarctica\", \"slug\": \"antarctica\", \"iso2\": \"AQ\", \"population\": 1106}\n" +
                "    ,\n" +
                "    {\"name\": \"Equatorial Guinea\", \"slug\": \"equatorial-guinea\", \"iso2\": \"GQ\", \"population\": 1308974}\n" +
                "    ,\n" +
                "    {\"name\": \"Jersey\", \"slug\": \"jersey\", \"iso2\": \"JE\", \"population\": 9308501}\n" +
                "    ,\n" +
                "    {\"name\": \"Lithuania\", \"slug\": \"lithuania\", \"iso2\": \"LT\", \"population\": 2801543}\n" +
                "    ,\n" +
                "    {\"name\": \"Tonga\", \"slug\": \"tonga\", \"iso2\": \"TO\", \"population\": 103197}\n" +
                "    ,\n" +
                "    {\"name\": \"United Kingdom\", \"slug\": \"united-kingdom\", \"iso2\": \"GB\", \"population\": 66460344}\n" +
                "    ,\n" +
                "    {\"name\": \"Antigua and Barbuda\", \"slug\": \"antigua-and-barbuda\", \"iso2\": \"AG\", \"population\": 96286}\n" +
                "    ,\n" +
                "    {\"name\": \"British Virgin Islands\", \"slug\": \"british-virgin-islands\", \"iso2\": \"VG\", \"population\": 30411}\n" +
                "    ,\n" +
                "    {\"name\": \"Thailand\", \"slug\": \"thailand\", \"iso2\": \"TH\", \"population\": 69428524}\n" +
                "    ,\n" +
                "    {\"name\": \"Bermuda\", \"slug\": \"bermuda\", \"iso2\": \"BM\", \"population\": 63973}\n" +
                "    ,\n" +
                "    {\"name\": \"Bulgaria\", \"slug\": \"bulgaria\", \"iso2\": \"BG\", \"population\": 7025037}\n" +
                "    ,\n" +
                "    {\"name\": \"Ecuador\", \"slug\": \"ecuador\", \"iso2\": \"EC\", \"population\": 17084357}\n" +
                "    ,\n" +
                "    {\"name\": \"Jordan\", \"slug\": \"jordan\", \"iso2\": \"JO\", \"population\": 9956011}\n" +
                "    ,\n" +
                "    {\"name\": \"Montenegro\", \"slug\": \"montenegro\", \"iso2\": \"ME\", \"population\": 631219}\n" +
                "    ,\n" +
                "    {\"name\": \"Palau\", \"slug\": \"palau\", \"iso2\": \"PW\", \"population\": 17907}\n" +
                "    ,\n" +
                "    {\"name\": \"Saint Pierre and Miquelon\", \"slug\": \"saint-pierre-and-miquelon\", \"iso2\": \"PM\", \"population\": 5888}\n" +
                "    ,\n" +
                "    {\"name\": \"Aruba\", \"slug\": \"aruba\", \"iso2\": \"AW\", \"population\": 105845}\n" +
                "    ,\n" +
                "    {\"name\": \"Colombia\", \"slug\": \"colombia\", \"iso2\": \"CO\", \"population\": 49648685}\n" +
                "    ,\n" +
                "    {\"name\": \"Ethiopia\", \"slug\": \"ethiopia\", \"iso2\": \"ET\", \"population\": 109224559}\n" +
                "    ,\n" +
                "    {\"name\": \"French Guiana\", \"slug\": \"french-guiana\", \"iso2\": \"GF\", \"population\": 290691}\n" +
                "    ,\n" +
                "    {\"name\": \"Nigeria\", \"slug\": \"nigeria\", \"iso2\": \"NG\", \"population\": 195874740}\n" +
                "    ,\n" +
                "    {\"name\": \"Pitcairn\", \"slug\": \"pitcairn\", \"iso2\": \"PN\", \"population\": 67}\n" +
                "    ,\n" +
                "    {\"name\": \"Grenada\", \"slug\": \"grenada\", \"iso2\": \"GD\", \"population\": 111454}\n" +
                "    ,\n" +
                "    {\"name\": \"Guinea\", \"slug\": \"guinea\", \"iso2\": \"GN\", \"population\": 12414318}\n" +
                "    ,\n" +
                "    {\"name\": \"Moldova\", \"slug\": \"moldova\", \"iso2\": \"MD\", \"population\": 2706049}\n" +
                "    ,\n" +
                "    {\"name\": \"Swaziland\", \"slug\": \"swaziland\", \"iso2\": \"SZ\", \"population\": 1136191}\n" +
                "    ,\n" +
                "    {\"name\": \"Benin\", \"slug\": \"benin\", \"iso2\": \"BJ\", \"population\": 11485048}\n" +
                "    ,\n" +
                "    {\"name\": \"Botswana\", \"slug\": \"botswana\", \"iso2\": \"BW\", \"population\": 2254126}\n" +
                "    ,\n" +
                "    {\"name\": \"Falkland Islands (Malvinas)\", \"slug\": \"falkland-islands-malvinas\", \"iso2\": \"FK\", \"population\": 3505}\n" +
                "    ,\n" +
                "    {\"name\": \"Niger\", \"slug\": \"niger\", \"iso2\": \"NE\", \"population\": 22442948}\n" +
                "    ,\n" +
                "    {\"name\": \"Peru\", \"slug\": \"peru\", \"iso2\": \"PE\", \"population\": 31989256}\n" +
                "    ,\n" +
                "    {\"name\": \"Portugal\", \"slug\": \"portugal\", \"iso2\": \"PT\", \"population\": 10283822}\n" +
                "    ,\n" +
                "    {\"name\": \"Togo\", \"slug\": \"togo\", \"iso2\": \"TG\", \"population\": 7889094}\n" +
                "    ,\n" +
                "    {\"name\": \"Brazil\", \"slug\": \"brazil\", \"iso2\": \"BR\", \"population\": 209469333}\n" +
                "    ,\n" +
                "    {\"name\": \"Ukraine\", \"slug\": \"ukraine\", \"iso2\": \"UA\", \"population\": 44622516}\n" +
                "    ,\n" +
                "    {\"name\": \"Bouvet Island\", \"slug\": \"bouvet-island\", \"iso2\": \"BV\", \"population\": 0}\n" +
                "    ,\n" +
                "    {\"name\": \"Uzbekistan\", \"slug\": \"uzbekistan\", \"iso2\": \"UZ\", \"population\": 32955400}\n" +
                "    ,\n" +
                "    {\"name\": \"Argentina\", \"slug\": \"argentina\", \"iso2\": \"AR\", \"population\": 44494502}\n" +
                "    ,\n" +
                "    {\"name\": \"Austria\", \"slug\": \"austria\", \"iso2\": \"AT\", \"population\": 8840521}\n" +
                "    ,\n" +
                "    {\"name\": \"Cape Verde\", \"slug\": \"cape-verde\", \"iso2\": \"CV\", \"population\": 543767}\n" +
                "    ,\n" +
                "    {\"name\": \"Chad\", \"slug\": \"chad\", \"iso2\": \"TD\", \"population\": 15477751}\n" +
                "    ,\n" +
                "    {\"name\": \"Somalia\", \"slug\": \"somalia\", \"iso2\": \"SO\", \"population\": 15008154}\n" +
                "    ,\n" +
                "    {\"name\": \"Turkey\", \"slug\": \"turkey\", \"iso2\": \"TR\", \"population\": 82319724}\n" +
                "    ,\n" +
                "    {\"name\": \"American Samoa\", \"slug\": \"american-samoa\", \"iso2\": \"AS\", \"population\": 55465}\n" +
                "    ,\n" +
                "    {\"name\": \"Isle of Man\", \"slug\": \"isle-of-man\", \"iso2\": \"IM\", \"population\": 85439}\n" +
                "    ,\n" +
                "    {\"name\": \"Korea (South)\", \"slug\": \"korea-south\", \"iso2\": \"KR\", \"population\": 51305186}\n" +
                "    ,\n" +
                "    {\"name\": \"Réunion\", \"slug\": \"réunion\", \"iso2\": \"RE\", \"population\": 901076}\n" +
                "    ,\n" +
                "    {\"name\": \"Albania\", \"slug\": \"albania\", \"iso2\": \"AL\", \"population\": 2866376}\n" +
                "    ,\n" +
                "    {\"name\": \"Samoa\", \"slug\": \"samoa\", \"iso2\": \"WS\", \"population\": 196130}\n" +
                "    ,\n" +
                "    {\"name\": \"Solomon Islands\", \"slug\": \"solomon-islands\", \"iso2\": \"SB\", \"population\": 652858}\n" +
                "    ,\n" +
                "    {\"name\": \"Dominica\", \"slug\": \"dominica\", \"iso2\": \"DM\", \"population\": 71625}\n" +
                "    ,\n" +
                "    {\"name\": \"Lesotho\", \"slug\": \"lesotho\", \"iso2\": \"LS\", \"population\": 2108132}\n" +
                "    ,\n" +
                "    {\"name\": \"Nauru\", \"slug\": \"nauru\", \"iso2\": \"NR\", \"population\": 12704}\n" +
                "    ,\n" +
                "    {\"name\": \"Nicaragua\", \"slug\": \"nicaragua\", \"iso2\": \"NI\", \"population\": 6465513}\n" +
                "    ,\n" +
                "    {\"name\": \"Slovenia\", \"slug\": \"slovenia\", \"iso2\": \"SI\", \"population\": 2073894}\n" +
                "    ,\n" +
                "    {\"name\": \"Congo (Brazzaville)\", \"slug\": \"congo-brazzaville\", \"iso2\": \"CG\", \"population\": 2470000}\n" +
                "    ,\n" +
                "    {\"name\": \"Fiji\", \"slug\": \"fiji\", \"iso2\": \"FJ\", \"population\": 902906}\n" +
                "    ,\n" +
                "    {\"name\": \"Latvia\", \"slug\": \"latvia\", \"iso2\": \"LV\", \"population\": 1927174}\n" +
                "    ,\n" +
                "    {\"name\": \"Eritrea\", \"slug\": \"eritrea\", \"iso2\": \"ER\", \"population\": 6213972}\n" +
                "    ,\n" +
                "    {\"name\": \"Greenland\", \"slug\": \"greenland\", \"iso2\": \"GL\", \"population\": 56025}\n" +
                "    ,\n" +
                "    {\"name\": \"Kenya\", \"slug\": \"kenya\", \"iso2\": \"KE\", \"population\": 51393010}\n" +
                "    ,\n" +
                "    {\"name\": \"Wallis and Futuna Islands\", \"slug\": \"wallis-and-futuna-islands\", \"iso2\": \"WF\", \"population\": 11060}\n" +
                "    ,\n" +
                "    {\"name\": \"Czech Republic\", \"slug\": \"czech-republic\", \"iso2\": \"CZ\", \"population\": 10629928}\n" +
                "    ,\n" +
                "    {\"name\": \"Guadeloupe\", \"slug\": \"guadeloupe\", \"iso2\": \"GP\", \"population\": 395700}\n" +
                "    ,\n" +
                "    {\"name\": \"Sri Lanka\", \"slug\": \"sri-lanka\", \"iso2\": \"LK\", \"population\": 21670000}\n" +
                "    ,\n" +
                "    {\"name\": \"Georgia\", \"slug\": \"georgia\", \"iso2\": \"GE\", \"population\": 3726549}\n" +
                "    ,\n" +
                "    {\"name\": \"Germany\", \"slug\": \"germany\", \"iso2\": \"DE\", \"population\": 82905782}\n" +
                "    ,\n" +
                "    {\"name\": \"Norfolk Island\", \"slug\": \"norfolk-island\", \"iso2\": \"NF\", \"population\": 2169}\n" +
                "    ,\n" +
                "    {\"name\": \"Slovakia\", \"slug\": \"slovakia\", \"iso2\": \"SK\", \"population\": 5446771}\n" +
                "    ,\n" +
                "    {\"name\": \"South Sudan\", \"slug\": \"south-sudan\", \"iso2\": \"SS\", \"population\": 10975920}\n" +
                "    ,\n" +
                "    {\"name\": \"Suriname\", \"slug\": \"suriname\", \"iso2\": \"SR\", \"population\": 575991}\n" +
                "    ,\n" +
                "    {\"name\": \"Barbados\", \"slug\": \"barbados\", \"iso2\": \"BB\", \"population\": 286641}\n" +
                "    ,\n" +
                "    {\"name\": \"Korea (North)\", \"slug\": \"korea-north\", \"iso2\": \"KP\", \"population\": 25881124}\n" +
                "    ,\n" +
                "    {\"name\": \"Libya\", \"slug\": \"libya\", \"iso2\": \"LY\", \"population\": 6958532}\n" +
                "    ,\n" +
                "    {\"name\": \"Mauritius\", \"slug\": \"mauritius\", \"iso2\": \"MU\", \"population\": 1265303}\n" +
                "    ,\n" +
                "    {\"name\": \"Paraguay\", \"slug\": \"paraguay\", \"iso2\": \"PY\", \"population\": 6956071}\n" +
                "    ,\n" +
                "    {\"name\": \"Saudi Arabia\", \"slug\": \"saudi-arabia\", \"iso2\": \"SA\", \"population\": 33699947}\n" +
                "    ,\n" +
                "    {\"name\": \"Guam\", \"slug\": \"guam\", \"iso2\": \"GU\", \"population\": 165768}\n" +
                "    ,\n" +
                "    {\"name\": \"Panama\", \"slug\": \"panama\", \"iso2\": \"PA\", \"population\": 4176873}\n" +
                "    ,\n" +
                "    {\"name\": \"United Arab Emirates\", \"slug\": \"united-arab-emirates\", \"iso2\": \"AE\", \"population\": 9630959}\n" +
                "]\n" +
                "\n" +
                "\n";
    }

    public List<Country> getAll() {
        return countryRepo.findAll();
    }

    public List<Country> getSorted() {
        List<Country> all = getAll();
        Collections.sort(all, (a, b) -> (a.getAvg_div_pop() < b.getAvg_div_pop() ? -1 : 1));
        return all.subList(65, 248);
    }

    public void test() {
        List<Country> list= getAll();
        for (Country c: list
             ) {
            c.setMako(convert(c.getSlug()));
        }
        countryRepo.saveAll(list);
    }

    private String convert(String slug) {
        if(slug==null) return null;
        char[] slugger = slug.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c: slugger) {
            if(c!='-'){
                stringBuilder.append(c);
            } else{
                stringBuilder.append('_');
            }
        }

        return stringBuilder.toString();
    }

//    public void test() {
//        for (Country c : countryRepo.findAll()) {
//            if(c.getIso2().equals("AN")) continue;
//            if(c.getRegion() == null) {
//                System.out.println(c.getName());
//                c.setRegion(set_region(c.getIso2()));
//                countryRepo.save(c);
//            }
//        }
//    }
//
//    private String set_region(String iso2) {
//        String url = "https://restcountries.eu/rest/v2/alpha/" + iso2;
//        Map<String, Object> map = restTemplate.getForObject(url, Map.class);
//        if (map == null || map.containsKey("status")) return null;
//        System.out.println(map.get("region").toString());
//        return map.get("region").toString();
//
//    }


}
