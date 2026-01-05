import { writable } from "svelte/store";

export const coinCountryMap = writable<Record<string, string>>();

coinCountryMap.set(
    {
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
})