import { BrowserRouter, Routes, Route } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import Overview from './pages/Overview';
import JobsList from './pages/JobsList';
import JobDetail from './pages/JobDetail';
import Queues from './pages/Queues';
import Workers from './pages/Workers';
import DeadLetter from './pages/DeadLetter';
import Settings from './pages/Settings';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Overview />} />
          <Route path="jobs" element={<JobsList />} />
          <Route path="jobs/:id" element={<JobDetail />} />
          <Route path="queues" element={<Queues />} />
          <Route path="workers" element={<Workers />} />
          <Route path="dead-letter" element={<DeadLetter />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
