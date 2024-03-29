import { Connect } from './Connect.tsx';
import { SearchAndExport } from './SearchAndExport.tsx';
import { ListUnfinishedUploads } from './ListUnfinishedUploads.tsx';
import { UploadFromFile } from './UploadFromFile.tsx';
import { Col, Nav, Row, Tab } from 'react-bootstrap';
import { ColorModeSwitch } from '@/features/dropbox/components/ColorModeSwitch.tsx';
import { UploadFromDropbox } from '@/features/dropbox/components/UploadFromDropbox.tsx';

export const Dropbox = () => {
  return (
    <div className={'main'}>
      <div className={'d-flex justify-content-between align-items-center mb-3'}>
        <Connect />
        <ColorModeSwitch />
      </div>

      <Tab.Container defaultActiveKey="uploadFromFile">
        <Row>
          <Col lg={2} className={'mb-3'}>
            <Nav variant="pills" className="flex-column">
              <Nav.Item>
                <Nav.Link eventKey="uploadFromFile">Ingest from file</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="uploadFromDropbox">
                  Ingest from Dropbox
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="listUnfinishedUploads">
                  Active ingestions
                </Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="search">Search and Export</Nav.Link>
              </Nav.Item>
            </Nav>
          </Col>
          <Col lg={10}>
            <Tab.Content>
              <Tab.Pane eventKey="uploadFromFile">
                <UploadFromFile />
              </Tab.Pane>
              <Tab.Pane eventKey="uploadFromDropbox">
                <UploadFromDropbox />
              </Tab.Pane>
              <Tab.Pane eventKey="listUnfinishedUploads">
                <ListUnfinishedUploads />
              </Tab.Pane>
              <Tab.Pane eventKey="search">
                <SearchAndExport />
              </Tab.Pane>
            </Tab.Content>
          </Col>
        </Row>
      </Tab.Container>
    </div>
  );
};
