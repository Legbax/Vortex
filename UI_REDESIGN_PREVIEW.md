# Propuesta de Rediseño: VortexUI (Native Android)

Esta es una adaptación nativa del diseño React "VortexUI" para Lancelot.

## 1. Paleta de Colores (colors.xml)

Estilo "Dark Cyberpunk" con acentos neón.

```xml
<resources>
    <!-- Fondos -->
    <color name="vortex_bg">#080A0F</color> <!-- Fondo principal casi negro -->
    <color name="vortex_surface">#0DFFFFFF</color> <!-- Blanco al 5% para efecto vidrio -->
    <color name="vortex_stroke">#1AFFFFFF</color> <!-- Borde sutil -->

    <!-- Acentos -->
    <color name="neon_green">#00FFAA</color> <!-- Color primario -->
    <color name="neon_purple">#7C6AF7</color> <!-- Secundario -->
    <color name="neon_orange">#F7A600</color> <!-- Alertas -->
    <color name="neon_red">#FF4444</color>    <!-- Error -->

    <!-- Texto -->
    <color name="text_primary">#FFFFFF</color>
    <color name="text_secondary">#80FFFFFF</color>
    <color name="text_tertiary">#4DFFFFFF</color>
</resources>
```

## 2. Estilos de Componentes (styles.xml)

```xml
<style name="Theme.Lancelot.Vortex" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="android:windowBackground">@color/vortex_bg</item>
    <item name="colorPrimary">@color/neon_green</item>
    <item name="colorSecondary">@color/neon_purple</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
</style>

<!-- Efecto Glass Card -->
<style name="VortexCard" parent="Widget.MaterialComponents.CardView">
    <item name="cardBackgroundColor">@color/vortex_surface</item>
    <item name="cardCornerRadius">16dp</item>
    <item name="cardElevation">0dp</item>
    <item name="strokeColor">@color/vortex_stroke</item>
    <item name="strokeWidth">1dp</item>
</style>

<!-- Texto Monospaced (Tipo consola) -->
<style name="VortexMonoText">
    <item name="android:fontFamily">monospace</item>
    <item name="android:textColor">@color/text_secondary</item>
    <item name="android:textSize">12sp</item>
</style>
```

## 3. Mockup de Layout (activity_main.xml)

Estructura visual propuesta:

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:background="@color/vortex_bg">

    <!-- Header: Título y Estado -->
    <LinearLayout
        android:orientation="horizontal"
        android:background="#CC080A0F"
        android:padding="16dp"> <!-- Efecto Blur simulado con alpha -->

        <ImageView android:src="@drawable/ic_shield_neon" />
        <TextView
            android:text="LANCELOT"
            android:letterSpacing="0.1"
            android:textColor="@color/text_primary"
            android:textStyle="bold"/>

        <Space android:layout_weight="1"/>

        <!-- Badge: ACTIVE -->
        <TextView
            android:text="● ACTIVE"
            android:textColor="@color/neon_green"
            android:background="@drawable/bg_badge_glass"/>
    </LinearLayout>

    <!-- Contenido Scrollable -->
    <ScrollView>
        <LinearLayout android:orientation="vertical" android:padding="16dp">

            <!-- Hero Card: Score -->
            <com.google.android.material.card.MaterialCardView
                style="@style/VortexCard">
                <LinearLayout android:padding="20dp">
                    <TextView text="MÓDULO VORTEX" android:textColor="@color/text_tertiary"/>
                    <TextView
                        android:text="82%"
                        android:textSize="48sp"
                        android:textColor="@color/neon_green"
                        android:fontFamily="monospace"/>
                    <TextView text="Evasión Estimada" android:textColor="@color/text_secondary"/>

                    <!-- Barra de progreso neón -->
                    <ProgressBar
                        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
                        android:progressDrawable="@drawable/progress_neon_gradient"
                        android:progress="82"/>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Card: Dispositivo -->
            <com.google.android.material.card.MaterialCardView
                style="@style/VortexCard"
                android:layout_marginTop="16dp">
                <LinearLayout android:padding="16dp">
                    <TextView text="DEVICE FINGERPRINT" android:textColor="@color/neon_purple"/>

                    <!-- Spinner estilizado -->
                    <Spinner
                        android:id="@+id/sp_profile"
                        android:background="@drawable/bg_input_glass"
                        android:padding="12dp"/>

                    <TextView text="CPU: Helio G80 · GPU: Mali-G52"
                              style="@style/VortexMonoText"/>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- Card: Identificadores -->
            <com.google.android.material.card.MaterialCardView
                style="@style/VortexCard"
                android:layout_marginTop="16dp">
                <LinearLayout android:padding="16dp">
                    <TextView text="IDENTIFIERS" android:textColor="@color/text_tertiary"/>

                    <!-- Input con estilo consola -->
                    <EditText
                        android:hint="IMEI 1"
                        android:textColor="@color/neon_green"
                        android:backgroundTint="@color/vortex_stroke"/>

                    <Button
                        android:text="RANDOMIZE"
                        android:backgroundTint="@color/vortex_surface"
                        android:textColor="@color/neon_green"/>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

        </LinearLayout>
    </ScrollView>

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:background="@color/vortex_bg"
        app:itemIconTint="@color/selector_neon"
        app:itemTextColor="@color/selector_neon"
        app:menu="@menu/bottom_nav_vortex" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

## 4. Visualización

*   **Fondo:** Oscuro profundo.
*   **Tarjetas:** Flotantes, semitransparentes, bordes muy finos.
*   **Tipografía:** Datos técnicos en monospaciado (verde/blanco).
*   **Botones:** Minimalistas, texto neón sobre fondo oscuro.

¿Apruebas proceder con la implementación de estos recursos (colors, styles, layout) en el proyecto?
