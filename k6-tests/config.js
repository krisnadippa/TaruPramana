export const SUPABASE_URL = __ENV.SUPABASE_URL || 'https://iupboxjgqbdgkarziuib.supabase.co';
export const SUPABASE_KEY = __ENV.SUPABASE_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml1cGJveGpncWJkZ2thcnppdWliIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2OTc3NjEsImV4cCI6MjA5NTI3Mzc2MX0.buFjSDKLNt8Pnn5mWSgdIrT80jyMkBU4bnU7fv7U1_M';
export const CHATBOT_URL = __ENV.CHATBOT_URL || 'https://krisnadipa-chatbot-tarupramana.hf.space';

export function getSupabaseHeaders() {
    return {
        'apikey': SUPABASE_KEY,
        'Authorization': `Bearer ${SUPABASE_KEY}`,
        'Content-Type': 'application/json',
    };
}
