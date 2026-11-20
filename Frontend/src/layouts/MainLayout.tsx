import { Outlet, ScrollRestoration } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'

function MainLayout() {
  return (
    <>
      <ScrollRestoration />
      <Navbar />
      <main><Outlet /></main>
      <Footer />
    </>
  )
}

export default MainLayout
