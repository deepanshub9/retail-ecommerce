/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 */

import { Test, TestingModule } from '@nestjs/testing';
import { CheckoutController } from './checkout.controller';
import { CheckoutService } from './checkout.service';
import { NotFoundException } from '@nestjs/common';
import { CheckoutRequest } from './models/CheckoutRequest';
import { Checkout } from './models/Checkout';
import { CheckoutSubmitted } from './models/CheckoutSubmitted';

describe('CheckoutController', () => {
  let controller: CheckoutController;
  let mockCheckoutService: jest.Mocked<CheckoutService>;

  beforeEach(async () => {
    const mockService = {
      get: jest.fn(),
      update: jest.fn(),
      submit: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [CheckoutController],
      providers: [
        {
          provide: CheckoutService,
          useValue: mockService,
        },
      ],
    }).compile();

    controller = module.get<CheckoutController>(CheckoutController);
    mockCheckoutService = module.get(CheckoutService);
  });

  describe('getCheckout', () => {
    it('should return checkout when found', async () => {
      // Given
      const customerId = 'customer123';
      const expectedCheckout: Checkout = {
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
            totalCost: 31.98,
          },
        ],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        subtotal: 31.98,
        tax: 5,
        shipping: 5.99,
        total: 42.97,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: 'standard',
        shippingRates: null,
      };

      mockCheckoutService.get.mockResolvedValue(expectedCheckout);

      // When
      const result = await controller.getCheckout(customerId);

      // Then
      expect(result).toBe(expectedCheckout);
      expect(mockCheckoutService.get).toHaveBeenCalledWith(customerId);
    });

    it('should throw NotFoundException when checkout not found', async () => {
      // Given
      const customerId = 'nonexistent';
      mockCheckoutService.get.mockResolvedValue(null);

      // When & Then
      await expect(controller.getCheckout(customerId)).rejects.toThrow(
        NotFoundException
      );
      expect(mockCheckoutService.get).toHaveBeenCalledWith(customerId);
    });

    it('should handle service errors', async () => {
      // Given
      const customerId = 'customer123';
      mockCheckoutService.get.mockRejectedValue(new Error('Database error'));

      // When & Then
      await expect(controller.getCheckout(customerId)).rejects.toThrow(
        'Database error'
      );
    });
  });

  describe('updateCheckout', () => {
    it('should update checkout successfully', async () => {
      // Given
      const customerId = 'customer123';
      const request: CheckoutRequest = {
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
          },
        ],
        shippingAddress: {
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com',
          address1: '123 Main St',
          city: 'Seattle',
          state: 'WA',
          zip: '98101',
        },
        deliveryOptionToken: 'standard',
      };

      const expectedCheckout: Checkout = {
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
            totalCost: 31.98,
          },
        ],
        shippingAddress: request.shippingAddress,
        subtotal: 31.98,
        tax: 5,
        shipping: 5.99,
        total: 42.97,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: 'standard',
        shippingRates: null,
      };

      mockCheckoutService.update.mockResolvedValue(expectedCheckout);

      // When
      const result = await controller.updateCheckout(customerId, request);

      // Then
      expect(result).toBe(expectedCheckout);
      expect(mockCheckoutService.update).toHaveBeenCalledWith(customerId, request);
    });

    it('should handle validation errors', async () => {
      // Given
      const customerId = 'customer123';
      const invalidRequest = {} as CheckoutRequest; // Invalid request
      mockCheckoutService.update.mockRejectedValue(new Error('Validation failed'));

      // When & Then
      await expect(controller.updateCheckout(customerId, invalidRequest)).rejects.toThrow(
        'Validation failed'
      );
    });

    it('should handle empty items array', async () => {
      // Given
      const customerId = 'customer123';
      const request: CheckoutRequest = {
        items: [],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      const expectedCheckout: Checkout = {
        items: [],
        shippingAddress: null,
        subtotal: 0,
        tax: -1,
        shipping: -1,
        total: 0,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: null,
        shippingRates: null,
      };

      mockCheckoutService.update.mockResolvedValue(expectedCheckout);

      // When
      const result = await controller.updateCheckout(customerId, request);

      // Then
      expect(result.items).toHaveLength(0);
      expect(result.subtotal).toBe(0);
      expect(result.total).toBe(0);
    });
  });

  describe('submitCheckout', () => {
    it('should submit checkout successfully', async () => {
      // Given
      const customerId = 'customer123';
      const expectedSubmission: CheckoutSubmitted = {
        orderId: 'order456',
        email: 'john@example.com',
        items: [
          {
            id: 'product1',
            name: 'Test Product',
            quantity: 2,
            price: 15.99,
            totalCost: 31.98,
          },
        ],
        subtotal: 31.98,
        tax: 5,
        shipping: 5.99,
        total: 42.97,
      };

      mockCheckoutService.submit.mockResolvedValue(expectedSubmission);

      // When
      const result = await controller.submitCheckout(customerId);

      // Then
      expect(result).toBe(expectedSubmission);
      expect(result.orderId).toBe('order456');
      expect(result.email).toBe('john@example.com');
      expect(mockCheckoutService.submit).toHaveBeenCalledWith(customerId);
    });

    it('should handle checkout not found during submission', async () => {
      // Given
      const customerId = 'nonexistent';
      mockCheckoutService.submit.mockRejectedValue(new Error('Checkout not found'));

      // When & Then
      await expect(controller.submitCheckout(customerId)).rejects.toThrow(
        'Checkout not found'
      );
    });

    it('should handle order service failures', async () => {
      // Given
      const customerId = 'customer123';
      mockCheckoutService.submit.mockRejectedValue(new Error('Order service unavailable'));

      // When & Then
      await expect(controller.submitCheckout(customerId)).rejects.toThrow(
        'Order service unavailable'
      );
    });

    it('should handle payment processing errors', async () => {
      // Given
      const customerId = 'customer123';
      mockCheckoutService.submit.mockRejectedValue(new Error('Payment processing failed'));

      // When & Then
      await expect(controller.submitCheckout(customerId)).rejects.toThrow(
        'Payment processing failed'
      );
    });
  });

  describe('parameter validation', () => {
    it('should handle special characters in customer ID', async () => {
      // Given
      const customerId = 'customer@123#special';
      mockCheckoutService.get.mockResolvedValue(null);

      // When & Then
      await expect(controller.getCheckout(customerId)).rejects.toThrow(
        NotFoundException
      );
      expect(mockCheckoutService.get).toHaveBeenCalledWith(customerId);
    });

    it('should handle very long customer IDs', async () => {
      // Given
      const customerId = 'a'.repeat(1000); // Very long ID
      mockCheckoutService.get.mockResolvedValue(null);

      // When & Then
      await expect(controller.getCheckout(customerId)).rejects.toThrow(
        NotFoundException
      );
    });

    it('should handle empty customer ID', async () => {
      // Given
      const customerId = '';
      mockCheckoutService.get.mockResolvedValue(null);

      // When & Then
      await expect(controller.getCheckout(customerId)).rejects.toThrow(
        NotFoundException
      );
    });
  });

  describe('concurrent operations', () => {
    it('should handle concurrent checkout updates', async () => {
      // Given
      const customerId = 'customer123';
      const request: CheckoutRequest = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10.00 }],
        shippingAddress: null,
        deliveryOptionToken: null,
      };

      const mockCheckout: Checkout = {
        items: [{ id: 'product1', name: 'Test', quantity: 1, price: 10.00, totalCost: 10.00 }],
        shippingAddress: null,
        subtotal: 10.00,
        tax: -1,
        shipping: -1,
        total: 10.00,
        paymentId: 'pay123',
        paymentToken: 'token456',
        deliveryOptionToken: null,
        shippingRates: null,
      };

      mockCheckoutService.update.mockResolvedValue(mockCheckout);

      // When - Simulate concurrent updates
      const promises = Array(5).fill(null).map(() => 
        controller.updateCheckout(customerId, request)
      );

      const results = await Promise.all(promises);

      // Then
      expect(results).toHaveLength(5);
      expect(mockCheckoutService.update).toHaveBeenCalledTimes(5);
      results.forEach(result => {
        expect(result.subtotal).toBe(10.00);
      });
    });
  });
});