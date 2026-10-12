import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'

import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'



export default createVuetify({
  components,
  directives,
  
  // 預設顏色,可套用在vuetify元件內
  theme: {
    defaultTheme: 'stockLight',

    themes: {
      stockLight: {
        dark: false,
        colors: {
          background: '#F4F6F8',
          surface: '#F2F3F5',
          
          primary: '#1565C0',
          secondary: '#455A64',
        },
      },
    },
  },
})