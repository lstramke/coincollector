/** Input field for authentication forms */
export type AuthField = {
        id: string;
        label: string;
        type?: 'text' | 'password' | 'email';
        value: string;
        required?: boolean;
        placeholder?: string;
    };