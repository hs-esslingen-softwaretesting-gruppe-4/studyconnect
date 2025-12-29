import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTableDataSource } from '@angular/material/table';

import { GenericTableComponent } from './generic-table.component';

describe('GenericTableComponent', () => {
  let component: GenericTableComponent<any>;
  let fixture: ComponentFixture<GenericTableComponent<any>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenericTableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GenericTableComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('dataSource', new MatTableDataSource([{ name: 'Alice' }]));
    fixture.componentRef.setInput('columns', [{ id: 'name', label: 'Name' }]);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
