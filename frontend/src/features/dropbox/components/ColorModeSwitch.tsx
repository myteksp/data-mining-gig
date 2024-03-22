import { Form } from 'react-bootstrap';
import { FiMoon, FiSun } from 'react-icons/fi';
import { useEffect, useState } from 'react';

export const ColorModeSwitch = () => {
  const [colorMode, setColorMode] = useState('dark');

  useEffect(() => {
    const colorLocalStorage = localStorage.getItem('colorMode');
    if (colorLocalStorage) {
      setColorMode(colorLocalStorage);
      document.documentElement.setAttribute('data-bs-theme', colorLocalStorage);
    }
  }, []);

  const modeToggle = () => {
    if (document.documentElement.getAttribute('data-bs-theme') == 'dark') {
      document.documentElement.setAttribute('data-bs-theme', 'light');
      setColorMode('light');
      localStorage.setItem('colorMode', 'light');
    } else {
      document.documentElement.setAttribute('data-bs-theme', 'dark');
      setColorMode('dark');
      localStorage.setItem('colorMode', 'dark');
    }
  };

  return (
    <div className={'d-flex align-items-center'}>
      {colorMode == 'dark' ? <FiMoon /> : <FiSun />}
      <Form.Check
        id={'color-mode-switch'}
        checked={colorMode !== 'dark'}
        type="switch"
        onChange={modeToggle}
        className={'ms-2'}
      />
    </div>
  );
};
