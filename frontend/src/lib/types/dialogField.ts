/** Input field for dialog/form components */
export type DialogField = {
    id: string;
    label: string;
    value: string | number;
    type?: "text" | "number" | "select" | "textarea" | "password";
    required?: boolean;
    disabled?: boolean;
    placeholder?: string;
    description?: string;
    options?: string[];
};