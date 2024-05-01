export interface City {
    name: string;
    lang: string;
    alt?: string;
}
export interface AltCity extends City {
    city: string;
}