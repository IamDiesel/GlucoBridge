/* ============================================================================
 *  glucoale_dummy.c  —  DUMMY / TEMPLATE (darf ins oeffentliche Repo)
 * ----------------------------------------------------------------------------
 *  Dies ist die STRUKTUR der JNI-Glue zur White-Box, jedoch OHNE jedes
 *  herstellerspezifische Artefakt und OHNE jeden sensiblen Algorithmus.
 *  Zweck: die Architektur der ALE-Anbindung dokumentieren (Forschung/Lehre),
 *  ohne eine funktionierende Umsetzung oder eine Reproduktions-Anleitung
 *  bereitzustellen. In dieser Fassung liefert der Code bewusst nur Fehlschlaege
 *  zurueck; die App faellt dann sauber auf die offene REST-Quelle zurueck.
 *
 *  VERWENDUNG (fuer die eigene, persoenliche Installation):
 *    1. Datei nach  data/source-ale/src/main/cpp/glucoale.c  kopieren/umbenennen
 *       (diese Zone ist git-ignoriert und gelangt NICHT ins Repo).
 *    2. Alle unten mit [DUMMY] markierten Platzhalter durch die fuer die
 *       EIGENE Installation selbst ermittelten Werte ersetzen und die als
 *       [DUMMY-STUB] markierten Funktionsruempfe selbst implementieren.
 *
 *  ------------------------------------------------------------------------
 *  LISTE DER DUMMY-ARTEFAKTE (alles hier ist Platzhalter, NICHT real):
 *    [DUMMY 1] VMA-Offsets            OFF_*            -> 0x000000
 *    [DUMMY 2] Whitebox-Erfolgscode   SKB_OK           -> 0x00000000
 *    [DUMMY 3] Key-Ladder-Parameter   U1_K*, U2_K*     -> 0
 *    [DUMMY 4] Name des exportierten White-Box-Symbols -> Platzhalter-String
 *    [DUMMY 5] ABI-Signaturen der White-Box-Funktionen -> neutralisiert
 *    [DUMMY-STUB 6] kp-Buendel-Parser (Formatlogik)    -> nicht implementiert
 *    [DUMMY-STUB 7] Provisioning-Ablauf (Key-Ladder)   -> nicht implementiert
 *  Zusaetzlich NICHT in diesem Template (separat, git-ignoriert):
 *    die native Bibliothek (*.so) sowie die statischen Blobs (cdfe1/cdfe2).
 *  ------------------------------------------------------------------------
 *  Es wird bewusst KEINE Anleitung zur Ermittlung/Beschaffung dieser Artefakte
 *  gegeben - weder hier noch im Forschungsbericht.
 * ============================================================================
 */
#define _GNU_SOURCE
#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "GlucoBridgeALE", __VA_ARGS__)

/* --- [DUMMY 1] VMA-Offsets in die White-Box ---------------------------------
 * Real: Byte-Abstaende interner Funktionen von der Ladebasis der Bibliothek.
 * Hier: Platzhalter. Fuer die eigene Installation selbst zu ermitteln und
 * einzutragen (das Wie ist bewusst nicht dokumentiert).                        */
#define OFF_GCM_DECRYPT   0x000000UL   /* [DUMMY] AEAD-Entschluesselung        */
#define OFF_GCM_ENCRYPT   0x000000UL   /* [DUMMY] AEAD-Verschluesselung        */
#define OFF_GCM_UNWRAP    0x000000UL   /* [DUMMY] Schluessel-Entpacken         */
#define OFF_PSS_SIGN      0x000000UL   /* [DUMMY] Signatur                     */
#define OFF_GETINSTANCE   0x000000UL   /* [DUMMY] Engine-Instanz               */
#define OFF_CREATEDATA    0x000000UL   /* [DUMMY] Datenobjekt aus Rohpuffer    */
#define SKB_OK            0x00000000u  /* [DUMMY 2] Erfolgs-Returncode         */

/* --- [DUMMY 3] Key-Ladder-Parameter -----------------------------------------
 * Real: konstante Parameter der beiden Unwrap-Schritte. Hier: Platzhalter.    */
static uint64_t U1_K3 = 0x0ULL, U1_K4 = 0x0ULL;   /* [DUMMY] Unwrap #1         */
static uint64_t U2_K3 = 0x0ULL, U2_K4 = 0x0ULL;   /* [DUMMY] Unwrap #2         */
static uint64_t U_S0  = 12ULL;   /* Nonce-Laenge (Standard-GCM, kein Geheimnis)*/
static uint64_t U_S2  = 16ULL;   /* Tag-Laenge   (Standard-GCM, kein Geheimnis)*/

/* --- [DUMMY 4] Name des exportierten White-Box-Symbols ----------------------*/
#define WB_EXPORTED_SYMBOL "__DUMMY_EXPORTED_SYMBOL__"  /* [DUMMY] Platzhalter  */

/* --- [DUMMY 5] ABI-Signaturen der White-Box-Funktionen ----------------------
 * Real: per Reverse-Engineering bestimmte Signaturen. Hier neutralisiert auf
 * generische Zeiger; die tatsaechliche Parameterbelegung ist NICHT dokumentiert.*/
typedef int (*wb_fn_t)(void);   /* [DUMMY] Platzhalter fuer alle WB-Funktionen */

static wb_fn_t gcm_decrypt, gcm_encrypt, gcm_unwrap, pss_sign, GetInstance, CreateData;
static void *g_engine=NULL, *g_data1=NULL, *g_data2=NULL;  /* Engine, Sign-Data, Crypt-Data */
static int g_init=0;                                       /* 1 nach erfolgreichem Init */
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

/* ===================== kp-PARSER (Struktur) =================================
 * Real: zerlegt die Server-Antwort (JSON-Feld "kp") in Schluesselobjekte und
 * einen Public-Key. Die konkrete Formatlogik ist hier als [DUMMY-STUB 6]
 * entfernt; nur die Datenstruktur-Huelle bleibt zur Illustration erhalten.    */
struct kp_result {
    /* [DUMMY] reale Felder (iv/ct/tag je Schluesselobjekt, PEM) hier entfernt */
    int placeholder;
};
static int kp_parse(const char *json, size_t jlen, struct kp_result *out){
    (void)json; (void)jlen;
    memset(out, 0, sizeof *out);
    LOG("kp_parse: [DUMMY-STUB] nicht implementiert");
    return -1;   /* [DUMMY-STUB 6] */
}

/* ---- Generische JNI-Helfer (kein Herstellerbezug) ------------------------- */
/* Kopiert ein jbyteArray in einen frisch allozierten Puffer (Aufrufer: free()).*/
static uint8_t* jb(JNIEnv *e, jbyteArray a, size_t *len){
    if(!a){ *len=0; return NULL; }
    jsize n=(*e)->GetArrayLength(e,a);
    uint8_t *b=(uint8_t*)malloc((size_t)n ? (size_t)n : 1);
    if(n) (*e)->GetByteArrayRegion(e,a,0,n,(jbyte*)b);
    *len=(size_t)n; return b;
}
/* Erzeugt ein neues jbyteArray aus einem C-Puffer. */
static jbyteArray mkarr(JNIEnv *e, const uint8_t *p, size_t n){
    jbyteArray a=(*e)->NewByteArray(e,(jsize)n);
    if(a) (*e)->SetByteArrayRegion(e,a,0,(jsize)n,(const jbyte*)p);
    return a;
}

/* ===================== JNI: nativeInit ======================================
 * Real: White-Box laden, Ladebasis aus einem exportierten Symbol ableiten,
 * Funktionszeiger aus den Offsets setzen, Engine + zwei Datenobjekte (aus
 * cdfe1/cdfe2) erzeugen. Hier [DUMMY-STUB]: liefert immer JNI_FALSE, damit die
 * App auf REST zurueckfaellt.                                                  */
JNIEXPORT jboolean JNICALL
Java_de_glucobridge_data_ale_NativeAleCryptoEngine_nativeInit(
        JNIEnv *env, jobject thiz, jbyteArray cdfe1, jbyteArray cdfe2){
    (void)env; (void)thiz; (void)cdfe1; (void)cdfe2;
    (void)gcm_decrypt; (void)gcm_encrypt; (void)gcm_unwrap; (void)pss_sign;
    (void)GetInstance; (void)CreateData; (void)g_engine; (void)g_data1; (void)g_data2;
    (void)g_init; (void)U1_K3; (void)U1_K4; (void)U2_K3; (void)U2_K4; (void)U_S0; (void)U_S2;
    LOG("nativeInit: [DUMMY-STUB] ALE nicht implementiert (Template) -> REST-Fallback");
    return JNI_FALSE;   /* [DUMMY-STUB] echte Init-Logik hier einsetzen        */
}

/* ===================== JNI: nativeBuildRequest ==============================
 * Real: Request-Body AEAD-verschluesseln (-> ct,tag) und Nachricht signieren
 * (-> sig); Rueckgabe [ct, tag, sig]. Hier [DUMMY-STUB]: liefert NULL.         */
JNIEXPORT jobjectArray JNICALL
Java_de_glucobridge_data_ale_NativeAleCryptoEngine_nativeBuildRequest(
        JNIEnv *env, jobject thiz, jbyteArray body, jbyteArray iv, jbyteArray msg){
    (void)env; (void)thiz; (void)body; (void)iv; (void)msg;
    LOG("nativeBuildRequest: [DUMMY-STUB] nicht implementiert");
    return NULL;   /* [DUMMY-STUB] */
}

/* ===================== JNI: nativeProvision =================================
 * Real: Key-Ladder -> Glukose-Klartext. Ablauf (kp entschluesseln -> kp_parse
 * -> zwei Unwrap-Schritte -> Session-Key -> Glukose entschluesseln) ist als
 * [DUMMY-STUB 7] entfernt. Hier: liefert NULL.                                 */
JNIEXPORT jbyteArray JNICALL
Java_de_glucobridge_data_ale_NativeAleCryptoEngine_nativeProvision(
        JNIEnv *env, jobject thiz,
        jbyteArray kpIv, jbyteArray kpCt, jbyteArray kpTag,
        jbyteArray gluIv, jbyteArray gluCt, jbyteArray gluTag){
    (void)env; (void)thiz; (void)kpIv; (void)kpCt; (void)kpTag;
    (void)gluIv; (void)gluCt; (void)gluTag;
    (void)jb; (void)mkarr; (void)kp_parse;   /* Helfer im Template ungenutzt   */
    struct kp_result kp; (void)kp;
    LOG("nativeProvision: [DUMMY-STUB] nicht implementiert");
    return NULL;   /* [DUMMY-STUB 7] */
}
