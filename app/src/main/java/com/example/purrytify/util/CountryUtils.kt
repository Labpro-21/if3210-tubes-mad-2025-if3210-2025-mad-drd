package com.example.purrytify.util

/**
 * Utility class for country-related operations
 */
object CountryUtils {
    
    /**
     * Data class representing a country
     */
    data class Country(
        val code: String,  // ISO 3166-1 alpha-2 country code
        val name: String   // Country name in English
    )
    
    /**
     * Complete list of countries with their ISO 3166-1 alpha-2 codes
     */
    val COUNTRIES = listOf(
        Country("AF", "Afghanistan"),
        Country("AX", "Åland Islands"),
        Country("AL", "Albania"),
        Country("DZ", "Algeria"),
        Country("AS", "American Samoa"),
        Country("AD", "Andorra"),
        Country("AO", "Angola"),
        Country("AI", "Anguilla"),
        Country("AQ", "Antarctica"),
        Country("AG", "Antigua and Barbuda"),
        Country("AR", "Argentina"),
        Country("AM", "Armenia"),
        Country("AW", "Aruba"),
        Country("AU", "Australia"),
        Country("AT", "Austria"),
        Country("AZ", "Azerbaijan"),
        Country("BS", "Bahamas"),
        Country("BH", "Bahrain"),
        Country("BD", "Bangladesh"),
        Country("BB", "Barbados"),
        Country("BY", "Belarus"),
        Country("BE", "Belgium"),
        Country("BZ", "Belize"),
        Country("BJ", "Benin"),
        Country("BM", "Bermuda"),
        Country("BT", "Bhutan"),
        Country("BO", "Bolivia"),
        Country("BQ", "Bonaire, Sint Eustatius and Saba"),
        Country("BA", "Bosnia and Herzegovina"),
        Country("BW", "Botswana"),
        Country("BV", "Bouvet Island"),
        Country("BR", "Brazil"),
        Country("IO", "British Indian Ocean Territory"),
        Country("BN", "Brunei Darussalam"),
        Country("BG", "Bulgaria"),
        Country("BF", "Burkina Faso"),
        Country("BI", "Burundi"),
        Country("CV", "Cabo Verde"),
        Country("KH", "Cambodia"),
        Country("CM", "Cameroon"),
        Country("CA", "Canada"),
        Country("KY", "Cayman Islands"),
        Country("CF", "Central African Republic"),
        Country("TD", "Chad"),
        Country("CL", "Chile"),
        Country("CN", "China"),
        Country("CX", "Christmas Island"),
        Country("CC", "Cocos (Keeling) Islands"),
        Country("CO", "Colombia"),
        Country("KM", "Comoros"),
        Country("CG", "Congo"),
        Country("CD", "Congo, Democratic Republic of the"),
        Country("CK", "Cook Islands"),
        Country("CR", "Costa Rica"),
        Country("CI", "Côte d'Ivoire"),
        Country("HR", "Croatia"),
        Country("CU", "Cuba"),
        Country("CW", "Curaçao"),
        Country("CY", "Cyprus"),
        Country("CZ", "Czechia"),
        Country("DK", "Denmark"),
        Country("DJ", "Djibouti"),
        Country("DM", "Dominica"),
        Country("DO", "Dominican Republic"),
        Country("EC", "Ecuador"),
        Country("EG", "Egypt"),
        Country("SV", "El Salvador"),
        Country("GQ", "Equatorial Guinea"),
        Country("ER", "Eritrea"),
        Country("EE", "Estonia"),
        Country("SZ", "Eswatini"),
        Country("ET", "Ethiopia"),
        Country("FK", "Falkland Islands (Malvinas)"),
        Country("FO", "Faroe Islands"),
        Country("FJ", "Fiji"),
        Country("FI", "Finland"),
        Country("FR", "France"),
        Country("GF", "French Guiana"),
        Country("PF", "French Polynesia"),
        Country("TF", "French Southern Territories"),
        Country("GA", "Gabon"),
        Country("GM", "Gambia"),
        Country("GE", "Georgia"),
        Country("DE", "Germany"),
        Country("GH", "Ghana"),
        Country("GI", "Gibraltar"),
        Country("GR", "Greece"),
        Country("GL", "Greenland"),
        Country("GD", "Grenada"),
        Country("GP", "Guadeloupe"),
        Country("GU", "Guam"),
        Country("GT", "Guatemala"),
        Country("GG", "Guernsey"),
        Country("GN", "Guinea"),
        Country("GW", "Guinea-Bissau"),
        Country("GY", "Guyana"),
        Country("HT", "Haiti"),
        Country("HM", "Heard Island and McDonald Islands"),
        Country("VA", "Holy See"),
        Country("HN", "Honduras"),
        Country("HK", "Hong Kong"),
        Country("HU", "Hungary"),
        Country("IS", "Iceland"),
        Country("IN", "India"),
        Country("ID", "Indonesia"),
        Country("IR", "Iran"),
        Country("IQ", "Iraq"),
        Country("IE", "Ireland"),
        Country("IM", "Isle of Man"),
        Country("IL", "Israel"),
        Country("IT", "Italy"),
        Country("JM", "Jamaica"),
        Country("JP", "Japan"),
        Country("JE", "Jersey"),
        Country("JO", "Jordan"),
        Country("KZ", "Kazakhstan"),
        Country("KE", "Kenya"),
        Country("KI", "Kiribati"),
        Country("KP", "North Korea"),
        Country("KR", "South Korea"),
        Country("KW", "Kuwait"),
        Country("KG", "Kyrgyzstan"),
        Country("LA", "Lao People's Democratic Republic"),
        Country("LV", "Latvia"),
        Country("LB", "Lebanon"),
        Country("LS", "Lesotho"),
        Country("LR", "Liberia"),
        Country("LY", "Libya"),
        Country("LI", "Liechtenstein"),
        Country("LT", "Lithuania"),
        Country("LU", "Luxembourg"),
        Country("MO", "Macao"),
        Country("MG", "Madagascar"),
        Country("MW", "Malawi"),
        Country("MY", "Malaysia"),
        Country("MV", "Maldives"),
        Country("ML", "Mali"),
        Country("MT", "Malta"),
        Country("MH", "Marshall Islands"),
        Country("MQ", "Martinique"),
        Country("MR", "Mauritania"),
        Country("MU", "Mauritius"),
        Country("YT", "Mayotte"),
        Country("MX", "Mexico"),
        Country("FM", "Micronesia"),
        Country("MD", "Moldova"),
        Country("MC", "Monaco"),
        Country("MN", "Mongolia"),
        Country("ME", "Montenegro"),
        Country("MS", "Montserrat"),
        Country("MA", "Morocco"),
        Country("MZ", "Mozambique"),
        Country("MM", "Myanmar"),
        Country("NA", "Namibia"),
        Country("NR", "Nauru"),
        Country("NP", "Nepal"),
        Country("NL", "Netherlands"),
        Country("NC", "New Caledonia"),
        Country("NZ", "New Zealand"),
        Country("NI", "Nicaragua"),
        Country("NE", "Niger"),
        Country("NG", "Nigeria"),
        Country("NU", "Niue"),
        Country("NF", "Norfolk Island"),
        Country("MK", "North Macedonia"),
        Country("MP", "Northern Mariana Islands"),
        Country("NO", "Norway"),
        Country("OM", "Oman"),
        Country("PK", "Pakistan"),
        Country("PW", "Palau"),
        Country("PS", "Palestine, State of"),
        Country("PA", "Panama"),
        Country("PG", "Papua New Guinea"),
        Country("PY", "Paraguay"),
        Country("PE", "Peru"),
        Country("PH", "Philippines"),
        Country("PN", "Pitcairn"),
        Country("PL", "Poland"),
        Country("PT", "Portugal"),
        Country("PR", "Puerto Rico"),
        Country("QA", "Qatar"),
        Country("RE", "Réunion"),
        Country("RO", "Romania"),
        Country("RU", "Russian Federation"),
        Country("RW", "Rwanda"),
        Country("BL", "Saint Barthélemy"),
        Country("SH", "Saint Helena, Ascension and Tristan da Cunha"),
        Country("KN", "Saint Kitts and Nevis"),
        Country("LC", "Saint Lucia"),
        Country("MF", "Saint Martin"),
        Country("PM", "Saint Pierre and Miquelon"),
        Country("VC", "Saint Vincent and the Grenadines"),
        Country("WS", "Samoa"),
        Country("SM", "San Marino"),
        Country("ST", "Sao Tome and Principe"),
        Country("SA", "Saudi Arabia"),
        Country("SN", "Senegal"),
        Country("RS", "Serbia"),
        Country("SC", "Seychelles"),
        Country("SL", "Sierra Leone"),
        Country("SG", "Singapore"),
        Country("SX", "Sint Maarten"),
        Country("SK", "Slovakia"),
        Country("SI", "Slovenia"),
        Country("SB", "Solomon Islands"),
        Country("SO", "Somalia"),
        Country("ZA", "South Africa"),
        Country("GS", "South Georgia and the South Sandwich Islands"),
        Country("SS", "South Sudan"),
        Country("ES", "Spain"),
        Country("LK", "Sri Lanka"),
        Country("SD", "Sudan"),
        Country("SR", "Suriname"),
        Country("SJ", "Svalbard and Jan Mayen"),
        Country("SE", "Sweden"),
        Country("CH", "Switzerland"),
        Country("SY", "Syrian Arab Republic"),
        Country("TW", "Taiwan"),
        Country("TJ", "Tajikistan"),
        Country("TZ", "Tanzania"),
        Country("TH", "Thailand"),
        Country("TL", "Timor-Leste"),
        Country("TG", "Togo"),
        Country("TK", "Tokelau"),
        Country("TO", "Tonga"),
        Country("TT", "Trinidad and Tobago"),
        Country("TN", "Tunisia"),
        Country("TR", "Turkey"),
        Country("TM", "Turkmenistan"),
        Country("TC", "Turks and Caicos Islands"),
        Country("TV", "Tuvalu"),
        Country("UG", "Uganda"),
        Country("UA", "Ukraine"),
        Country("AE", "United Arab Emirates"),
        Country("GB", "United Kingdom"),
        Country("US", "United States"),
        Country("UM", "United States Minor Outlying Islands"),
        Country("UY", "Uruguay"),
        Country("UZ", "Uzbekistan"),
        Country("VU", "Vanuatu"),
        Country("VE", "Venezuela"),
        Country("VN", "Viet Nam"),
        Country("VG", "Virgin Islands, British"),
        Country("VI", "Virgin Islands, U.S."),
        Country("WF", "Wallis and Futuna"),
        Country("EH", "Western Sahara"),
        Country("YE", "Yemen"),
        Country("ZM", "Zambia"),
        Country("ZW", "Zimbabwe")
    )
    
    /**
     * Get country name from country code
     * @param code ISO 3166-1 alpha-2 country code
     * @return Country name or null if not found
     */
    fun getCountryNameFromCode(code: String): String? {
        return COUNTRIES.find { it.code == code }?.name
    }
    
    /**
     * Get country from country code
     * @param code ISO 3166-1 alpha-2 country code
     * @return Country object or null if not found
     */
    fun getCountryFromCode(code: String): Country? {
        return COUNTRIES.find { it.code == code }
    }
    
    /**
     * Filter countries by name (search)
     * @param query Search query
     * @return List of countries that match the query
     */
    fun searchCountries(query: String): List<Country> {
        val searchLower = query.lowercase()
        return COUNTRIES.filter { 
            it.name.lowercase().contains(searchLower) || it.code.lowercase().contains(searchLower)
        }
    }
    
    /**
     * Convert country code to flag emoji
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return Flag emoji for the country
     */
    fun getFlagEmoji(countryCode: String): String {
        // Regional indicator symbols are Unicode characters used to create flag emojis
        // Each letter A-Z is represented by a Unicode character from U+1F1E6 to U+1F1FF
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}