import { writable } from "svelte/store";

const countryToCode: Record<string, string> = {
    "Österreich": "AT",
    "Belgien": "BE",
    "Zypern": "CY",
    "Deutschland": "DE",
    "Estland": "EE",
    "Spanien": "ES",
    "Finnland": "FI",
    "Frankreich": "FR",
    "Griechenland": "GR",
    "Irland": "IE",
    "Italien": "IT",
    "Litauen": "LT",
    "Luxemburg": "LU",
    "Lettland": "LV",
    "Malta": "MT",
    "Niederlande": "NL",
    "Portugal": "PT",
    "Slowenien": "SI",
    "Slowakei": "SK",
    "San Marino": "SM",
    "Vatikanstadt": "VA",
    "Monaco": "MC",
    "Andorra": "AD"
};

const codeToCountry: Record<string, string> = Object.fromEntries(
    Object.entries(countryToCode).map(([country, code]) => [code, country])
);

export const coinCountryMap = writable<Record<string, string>>(countryToCode);
export const coinCountryCodeMap = writable<Record<string, string>>(codeToCountry);